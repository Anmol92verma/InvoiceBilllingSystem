package com.dashlabs.invoicemanagement.view.customers

import com.dashlabs.invoicemanagement.State
import com.dashlabs.invoicemanagement.StateDistrict
import com.dashlabs.invoicemanagement.databaseconnection.CustomersTable
import com.dashlabs.invoicemanagement.databaseconnection.Database
import com.dashlabs.invoicemanagement.view.invoices.InvoicesController
import com.google.gson.Gson
import io.reactivex.Single
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import io.reactivex.schedulers.Schedulers
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.TableView
import javafx.scene.input.KeyCode
import org.fxmisc.wellbehaved.event.EventPattern
import org.fxmisc.wellbehaved.event.InputMap
import org.fxmisc.wellbehaved.event.Nodes
import tornadofx.*
import javax.json.Json

class SearchCustomerView : View("Search Customers") {
    private val invoicesController: InvoicesController by inject()
    private val viewModel = SerchInvoiceViewModel()
    val state = getStates()
    private var districtView: Field? = null

    private fun getStates(): List<State> {
        val stream = javaClass.getResourceAsStream("/states-and-districts.json")
        val state = Gson().fromJson(Json.createReader(stream).readObject().toPrettyString(), StateDistrict::class.java)
        return state.states
    }

    override val root = vbox {
        Nodes.addInputMap(this, InputMap.sequence(
                InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER)) { e ->
                    searchCustomers()
                }
        ))


        invoicesController.customersListObservable.addListener { observable, oldValue, newValue ->
            totalBalanceByRegion()
        }
        hbox {
            // search by region
            vbox {
                label {
                    text = "Search for invoice by region"
                    vboxConstraints { margin = Insets(10.0) }
                }
                form {
                    fieldset {
                        field("State") {
                            combobox(viewModel.state, state.map { it.state }) {
                                selectionModel.selectedIndex
                            }.validator {
                                if (it.isNullOrBlank()) error("Please select the state") else null
                            }
                        }

                        viewModel.state.onChange {
                            this@SearchCustomerView.districtView?.let {
                                viewModel.district.value = null
                                it.removeFromParent()
                            }
                            getDistrictView(this@fieldset)
                        }
                    }
                }

                form {
                    fieldset {
                        field("Address") {
                            textfield(viewModel.address)
                        }
                    }
                }

                button("Search") {
                    vboxConstraints { margin = Insets(10.0) }
                    alignment = Pos.BOTTOM_RIGHT
                    setOnMouseClicked {
                        searchCustomers()
                    }
                }

                label(viewModel.totalPrice) {
                    vboxConstraints { margin = Insets(10.0) }
                    style = "-fx-font-weight: bold"
                }

            }
        }

        vbox {
            this.add(getCustomersView())
        }
    }

    private fun searchCustomers() {
        viewModel.totalPrice.value = null
        invoicesController.searchCustomers(viewModel.state.value, viewModel.district.value, viewModel.address.value
                ?: "")
    }

    private fun totalBalanceByRegion() {
        Single.fromCallable {
            var totalPrice = 0.0
            invoicesController.customersListObservable.value.map {
                Database.listInvoices(it.customerId)
            }.forEach {
                it?.map { it.outstandingAmount }?.sum()?.let {
                    totalPrice += it
                }
            }
            totalPrice
        }.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform()).subscribe { t1, t2 ->
                    t1?.let {
                        viewModel.totalPrice.value = "Total amount for the region $it"
                    }
                }
    }

    private fun getCustomersView(): TableView<CustomersTable.MeaningfulCustomer> {
        return tableview<CustomersTable.MeaningfulCustomer>(invoicesController.customersListObservable) {
            columnResizePolicy = SmartResize.POLICY
            stylesheets.add("jfx-table-view.css")

            vboxConstraints { margin = Insets(20.0) }
            column("Customer Name", CustomersTable.MeaningfulCustomer::customerName).remainingWidth()
            column("Amount Due", CustomersTable.MeaningfulCustomer::amountDue).remainingWidth()
            column("Address", CustomersTable.MeaningfulCustomer::address).remainingWidth()
            column("State", CustomersTable.MeaningfulCustomer::state).remainingWidth()
            column("District", CustomersTable.MeaningfulCustomer::district).remainingWidth()
            onDoubleClick {
                this.selectedItem?.let {
                    CustomerDetailView(it).openWindow()
                }
            }
        }
    }

    private fun getDistrictView(it: Fieldset) {
        this@SearchCustomerView.districtView = field("District") {
            tag = "district"
            val state = state.firstOrNull { it.state.equals(viewModel.state.value) }
            state?.let {
                combobox(viewModel.district, state.districts).validator {
                    if (it.isNullOrBlank()) error("Please select the District!") else null
                }
            }
        }
        this@SearchCustomerView.districtView?.addTo(it)
    }
}


class SerchInvoiceViewModel : ItemViewModel<Customer>(Customer()) {
    val state = bind(Customer::state)
    val district = bind(Customer::districtProperty)
    val totalPrice = bind(Customer::search)
    val address = bind(Customer::address)
}