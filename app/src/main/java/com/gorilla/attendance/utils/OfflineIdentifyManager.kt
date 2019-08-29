package com.gorilla.attendance.utils

import android.util.Base64
import com.gorilla.attendance.data.model.DeviceIdentitiesData
import com.gorilla.attendance.data.model.Employees
import com.gorilla.attendance.data.model.Visitors
import gorilla.fdr.Identify

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/15
 * Description:
 */
class OfflineIdentifyManager {
    var mIdentifyEmployees: Identify? = null
    var mIdentifyVisitors: Identify? = null

    var isInitPreIdentities = false

//    fun clearIdentify() {
//        mIdentifyEmployees?.free()
//        mIdentifyEmployees = null
//
//        mIdentifyVisitors?.free()
//        mIdentifyVisitors = null
//    }

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
                identify.addModel(employee.intId, imageData)
                //Timber.d("Add employee model, return value: $addModelReturn")
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

                //Timber.d("visitor.intId: ${visitor.intId}")
                identify.addModel(visitor.intId, imageData)
                //Timber.d("Add visitor model, return value: $addModelReturn")
            }
        }
    }

    fun initWithIdentities(identities: DeviceIdentitiesData?) {
        var addModelReturn: Int

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
            }
        }
    }
}