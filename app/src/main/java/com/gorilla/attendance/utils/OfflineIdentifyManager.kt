package com.gorilla.attendance.utils

import android.util.Base64
import com.gorilla.attendance.data.model.DeviceIdentitiesData
import com.gorilla.attendance.data.model.Employees
import com.gorilla.attendance.data.model.Visitors
import gorilla.fdr.Identify
import timber.log.Timber
import java.lang.Exception

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/15
 * Description:
 */
class OfflineIdentifyManager {
    var mIdentifyEmployees: Identify? = null
    var mIdentifyVisitors: Identify? = null

    var mSingleEmployeeIdentify: Identify? = null
    var mSingleVisitorIdentify: Identify? = null

    private var isInitPreIdentities = false

    val employeeList = ArrayList<Employees>()
    val visitorList = ArrayList<Visitors>()

    var isInitSingleIdentifier = false

//    fun clearIdentify() {
//        mIdentifyEmployees?.free()
//        mIdentifyEmployees = null
//
//        mIdentifyVisitors?.free()
//        mIdentifyVisitors = null
//    }

    fun initSingleEmployeeIdentify(intId: Int) {
        Timber.d("initSingleEmployeeIdentify()")

        isInitSingleIdentifier = true

        try {
            if (mSingleEmployeeIdentify != null) {
                mSingleEmployeeIdentify?.free()
                mSingleEmployeeIdentify = null
            }

            mSingleEmployeeIdentify = Identify(DeviceUtils.APP_INTERNAL_BIN_FOLDER)

            for (employee in employeeList) {
                if (employee.intId == intId) {
                    val imageData = Base64.decode(employee.model, Base64.DEFAULT)

                    if (employee.bapModelId.isNullOrEmpty()) {
                        employee.bapModelId = "9999"   // Temp value
                    }

                    mSingleEmployeeIdentify?.addModel(employee.intId, imageData)
                    break
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }

        isInitSingleIdentifier = false
    }

    fun initSingleVisitorIdentify(intId: Int) {
        Timber.d("initSingleVisitorIdentify()")

        isInitSingleIdentifier = true

        try {
            if (mSingleVisitorIdentify != null) {
                mSingleVisitorIdentify?.free()
                mSingleVisitorIdentify = null
            }

            mSingleVisitorIdentify = Identify(DeviceUtils.APP_INTERNAL_BIN_FOLDER)

            for (visitor in visitorList) {
                if (visitor.intId == intId) {
                    val imageData = Base64.decode(visitor.model, Base64.DEFAULT)

                    if (visitor.bapModelId.isNullOrEmpty()) {
                        visitor.bapModelId = "9999999"   // Temp value
                    }

                    mSingleVisitorIdentify?.addModel(visitor.intId, imageData)
                    break
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }

        isInitSingleIdentifier = false
    }

    fun initWithPreIdentities(identities: DeviceIdentitiesData?) {
        if (isInitPreIdentities) {
            return
        }

        isInitPreIdentities = true
        initWithIdentities(identities)
    }

    fun insertEmployees(list: ArrayList<Employees>) {
        mIdentifyEmployees?.let { identify ->
            for (employee in list) {
                //Timber.d("Employee model = ${employee.model}")
                //Timber.d("Employee bap model ID = ${employee.bapModelId}")

                val imageData = Base64.decode(employee.model, Base64.DEFAULT)
                //Timber.d("imageData = $imageData")
                //Timber.d("imageData.size = ${imageData.size}")

                if (employee.bapModelId.isNullOrEmpty()) {
                    employee.bapModelId = "9999"   // Temp value
                }

                //Timber.d("employee.intId: ${employee.intId}")
                //Timber.d("Add employee model, return value: $addModelReturn")

                if (!employeeList.contains(employee)) {
                    identify.addModel(employee.intId, imageData)
                    employeeList.add(employee)
                } else {
                    identify.removeModel(employee.intId)
                    employeeList[employeeList.indexOf(employee)] = employee
                    identify.addModel(employee.intId, imageData)
                }
            }
        }
    }

    fun removeEmployee(intId: Int) {
        for (employee in employeeList) {
            if (employee.intId == intId) {
                employeeList.remove(employee)
                mIdentifyEmployees?.let { identify ->
                    identify.removeModel(employee.intId)
                }
                return
            }
        }
    }

    fun insertVisitors(list: ArrayList<Visitors>) {
        mIdentifyVisitors?.let { identify ->
            for (visitor in list) {
                //Timber.d("Visitor model = ${visitor.model}")
                //Timber.d("Visitor bap model ID = ${visitor.bapModelId}")

                val imageData = Base64.decode(visitor.model, Base64.DEFAULT)
                //Timber.d("imageData = $imageData")
                //Timber.d("imageData.size = ${imageData.size}")

                if (visitor.bapModelId.isNullOrEmpty()) {
                    visitor.bapModelId = "9999999"   // Temp value
                }

                if (!visitorList.contains(visitor)) {
                    identify.addModel(visitor.intId, imageData)
                    visitorList.add(visitor)
                } else {
                    identify.removeModel(visitor.intId)
                    visitorList[visitorList.indexOf(visitor)] = visitor
                    identify.addModel(visitor.intId, imageData)
                }
            }
        }
    }

    fun initWithIdentities(identities: DeviceIdentitiesData?) {
        var addModelReturn: Int

        if (mIdentifyEmployees != null && mIdentifyVisitors != null) {
            return
        }

        // Employees identities
        mIdentifyEmployees = Identify(DeviceUtils.APP_INTERNAL_BIN_FOLDER)
        mIdentifyEmployees?.let { identify ->
            identities?.employees?.let {
                for (employee in it) {
                    //Timber.d("Employee model = ${employee.model}")
                    //Timber.d("Employee bap model ID = ${employee.bapModelId}")

                    val imageData = Base64.decode(employee.model, Base64.DEFAULT)
                    //Timber.d("imageData = $imageData")
                    //Timber.d("imageData.size = ${imageData.size}")

                    if (employee.bapModelId.isNullOrEmpty()) {
                        employee.bapModelId = "9999"   // Temp value
                    }

                    //Timber.d("employee.intId: ${employee.intId}")
                    addModelReturn = identify.addModel(employee.intId, imageData)
                    //Timber.d("Add employee model, return value: $addModelReturn")
                }


                employeeList.addAll(it)
            }
        }

        // Visitor identities
        mIdentifyVisitors = Identify(DeviceUtils.APP_INTERNAL_BIN_FOLDER)
        mIdentifyVisitors?.let { identify ->
            identities?.visitors?.let {
                for (visitor in it) {
                    //Timber.d("Visitor model = ${visitor.model}")
                    //Timber.d("Visitor bap model ID = ${visitor.bapModelId}")

                    val imageData = Base64.decode(visitor.model, Base64.DEFAULT)
                    //Timber.d("imageData = $imageData")
                    //Timber.d("imageData.size = ${imageData.size}")

                    if (visitor.bapModelId.isNullOrEmpty()) {
                        visitor.bapModelId = "9999999"   // Temp value
                    }

                    //Timber.d("visitor.intId: ${visitor.intId}")
                    addModelReturn = identify.addModel(visitor.intId, imageData)
                    //Timber.d("Add visitor model, return value: $addModelReturn")
                }

                visitorList.addAll(it)
            }
        }
    }
}