package com.dashlabs.invoicemanagement.view.customers

import com.dashlabs.invoicemanagement.databaseconnection.CustomersTable
import com.dashlabs.invoicemanagement.databaseconnection.Database
import com.dashlabs.invoicemanagement.view.invoices.toMeaningFulCustomer
import io.reactivex.Single
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import io.reactivex.schedulers.Schedulers
import javafx.beans.property.Property
import javafx.beans.property.SimpleListProperty
import javafx.collections.FXCollections
import tornadofx.*

class CustomersController : Controller() {

    val customersListObserver = SimpleListProperty<CustomersTable.MeaningfulCustomer>()

    fun requestForCustomers() {
        val listOfCustomers = Database.listCustomers()?.map { it.toMeaningFulCustomer() }
        runLater {
            listOfCustomers?.let {
                customersListObserver.set(FXCollections.observableArrayList(it))
            }
        }
    }

    fun searchProduct(search: String) {
        Single.create<List<CustomersTable.MeaningfulCustomer>> {
            try {
                val listOfCustomers = Database.listCustomers(search = search)
                listOfCustomers?.let { it1 -> it.onSuccess(it1.map { it.toMeaningFulCustomer() }) }
            } catch (ex: Exception) {
                it.onError(ex)
            }
        }.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe { t1, t2 ->
                    t1?.let {
                        customersListObserver.set(FXCollections.observableArrayList<CustomersTable.MeaningfulCustomer>(it))
                    }
                }
    }

    fun addCustomer(customerName: Property<String>, address: Property<String>, state: Property<String>, district: Property<String>) {
        Single.create<CustomersTable> {
            try {
                if (customerName.value.isNullOrEmpty() || address.value.isNullOrEmpty()|| state.value.isNullOrEmpty() || district.value.isNullOrEmpty()) {
                    it.onError(Exception())
                } else {
                    val customer = Customer()
                    customer.name = customerName.value
                    customer.address = address.value
                    customer.state = state.value
                    customer.district  = district.value
                    Database.createCustomer(customer)?.let { it1 -> it.onSuccess(it1) }
                }
            } catch (ex: Exception) {
                it.onError(ex)
            }
        }.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe { t1, t2 ->
                    t1?.let {
                        customersListObserver.add(it.toMeaningFulCustomer())
                        print(it)
                    }
                    t2?.let {
                        print(it)
                        it.message?.let { it1 -> warning(it1).show() }
                    }
                }
    }

    fun deleteCustomer(item: CustomersTable.MeaningfulCustomer) {
        Single.fromCallable<Boolean> {
            try {
                Database.deleteCustomer(item.customerId)
            } catch (ex: Exception) {
                throw ex
            }
        }.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe { t1, t2 ->
                    t1?.let {
                        requestForCustomers()
                    }
                    t2?.let {
                        print(it)
                        it.message?.let { it1 -> warning(it1).show() }
                    }
                }
    }

}

class CustomerViewModel : ItemViewModel<Customer>(Customer()) {
    val customerName = bind(Customer::nameProperty)
    val searchName = bind(Customer::searchProperty)
    val address = bind(Customer::address)
    val state = bind(Customer::state)
    val district = bind(Customer::districtProperty)
}