package com.example.mmt.Utils

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.database.getLongOrNull
import com.example.mmt.BuildConfig
import com.example.mmt.Const.Project
import com.example.mmt.Model.Excel
import com.example.mmt.Model.Mpesa
import com.example.mmt.Model.Smses
import com.example.mmt.Model.Texts
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class mpesaUtils(val context: Context) {
    fun dataUtil(ls:ArrayList<Texts>):Mpesa{
        var excelData:ArrayList<Excel> = ArrayList()
        var sentCash=0.0
        var withdrawnCash=0.0
        var balanceCheck=0.0
        var paidCash=0.0
        var receivedCash=0.0
        var airTimeCash=0.0
        ls.forEach {
            var Amount=""
            var transactionType=""
            var details=""
            var tranId=""
            if(it.address.equals("MPESA")){
                if(it.text!!.contains("sent")){
                    val msTxt=it.text.split(" ")
                    Amount=msTxt.find { it.startsWith("Ksh") }!!.substring(3).trim()
                    sentCash=sentCash+Amount.replace(",","").toDouble()
                    transactionType="Sent"
                    val startIndex=msTxt.indexOf("sent")+2
                    val endIndex=msTxt.indexOf("on")
                    tranId=msTxt[0].trim()
                    details=msTxt.subList(startIndex,endIndex).joinToString(" ")
                }
                else if (it.text!!.contains("bought")){
                    val msTxt=it.text.split(" ")
                    tranId=msTxt[0].trim()
                    Amount=msTxt.find { it.startsWith("Ksh") }!!.substring(3).trim()
                    airTimeCash=airTimeCash+Amount.replace(",","").toDouble()
                    transactionType="Airtime Top Up"
                    details="Purchase of airtime"
                }
                else if (it.text!!.contains("received")){
                    val msTxt=it.text.split(" ")
                    tranId=msTxt[0].trim()
                    val startIndex=msTxt.indexOf("from")+1
                    val endIndex=msTxt.indexOf("on")
                    details=msTxt.subList(startIndex,endIndex).joinToString(" ")
                    Amount=msTxt.find { it.startsWith("Ksh") }!!.substring(3).trim()              // transactionType="Received"
                    receivedCash=receivedCash+Amount.replace(",","").toDouble()
                    transactionType="Received"
                }
                else if (it.text!!.contains("paid")){
                    val msTxt=it.text.split(" ")
                    tranId=msTxt[0].trim()
                    val startIndex=msTxt.indexOf("to")+1
                    val endIndex=msTxt.indexOf("on")
                    details=msTxt.subList(startIndex,endIndex).joinToString(" ")
                    Amount=msTxt.find { it.startsWith("Ksh") }!!.substring(3).trim()
                    paidCash=paidCash+Amount.replace(",","").toDouble()
                    transactionType="Paid"
                }
                else if (it.text!!.contains("AMWithdraw")||it.text!!.contains("PMWithdraw")){
                    val msTxt=it.text.split(" ")
                    val startIndex=msTxt.indexOf("from")+1
                    val endIndex=msTxt.indexOf("New")
                    details=msTxt.subList(startIndex,endIndex).joinToString(" ")
                    tranId=msTxt[0].trim()
                    Amount=msTxt.find { it.startsWith("Ksh") }!!.substring(3).trim()
                    withdrawnCash=withdrawnCash+Amount.replace(",","").toDouble()
                    transactionType="Withdrew"
                }
                else if (it.text!!.contains("account balance")){
                    val msTxt=it.text.split(" ")
                    tranId=msTxt[0].trim()
                    Amount=msTxt.find { it.startsWith("Ksh") }!!.substring(3).trim()
                    transactionType="Balance"
                    details="Enquiring for Account Balance"
                    balanceCheck=balanceCheck+1
                }
                else{
                    if(!it.text.contains("modified PIN")){
                        transactionType="Failed"
                        Amount="0.00"
                        tranId="None"
                        details="Transaction failed"
                    }
                }
                val fAmount=if(Amount.isNotEmpty()) Amount.replace(",","").toDouble() else 0.0
                excelData.add(Excel(tranId,it.dateRecieved!!.trim(),transactionType,details,fAmount))
            }
        }
        return Mpesa(excelData, arrayOf(sentCash,airTimeCash,receivedCash,paidCash,withdrawnCash,balanceCheck))
    }
    fun prepExcel(excelData:ArrayList<Excel>,path:String){
        val workbook: Workbook =  XSSFWorkbook()
        var cell: Cell? =null
        var sheet: Sheet = workbook.createSheet("mpesa")
        val row: Row = sheet.createRow(0)
        Project.columns.forEachIndexed{ index, element->
            cell = row.createCell(index);
            cell?.setCellValue(element);
        }
        for (x in 0..excelData.size-1){
            val item=excelData[x]
            val rowData = sheet.createRow(x + 1)
            cell = rowData.createCell(0)
            cell?.setCellValue(item.tranId)
            cell = rowData.createCell(1)
            cell?.setCellValue(item.dateRecieved)
            cell = rowData.createCell(2)
            cell?.setCellValue(item.transactionType)
            cell = rowData.createCell(3)
            cell?.setCellValue(item.details)
            cell = rowData.createCell(4)
            cell?.setCellValue(item.Amount)
        }
        this.context.openFileOutput("my_mpesa_excel.xlsx", Context.MODE_PRIVATE).use { output ->
            workbook.write(output)
            output.close()
        }
        val ext=path+"/my_mpesa_excel.xlsx"
        val excelFile = File(ext)
        val uriFile=if (Build.VERSION.SDK_INT< Build.VERSION_CODES.N) Uri.fromFile(excelFile) else FileProvider.getUriForFile(context,
            BuildConfig.APPLICATION_ID+".provider",excelFile )
        val share= Intent()
        share.setAction(Intent.ACTION_SEND)
        share.putExtra(Intent.EXTRA_STREAM, uriFile)
        share.setType("*/*")
        context.startActivity(share)
    }
    fun smsesUtil(): Smses{
        //ArrayList<Texts>
        val smses=ArrayList<Texts>()
        var count=0
        val uri= Uri.parse("content://sms/inbox")
        val cursor: Cursor? = context.getContentResolver().query(uri, null, null, null, null)
        while (cursor!!.moveToNext()){
            val sdf = SimpleDateFormat("dd/MM/yyyy")
            val netDate = Date(cursor.getString(cursor.getColumnIndexOrThrow("date_sent")).toLong())
            val txt=Texts(id =cursor.getLongOrNull(cursor.getColumnIndexOrThrow("_id")),
                address =cursor.getString(cursor.getColumnIndexOrThrow("address")), text = cursor.getString(cursor.getColumnIndexOrThrow("body")),
                dateRecieved = sdf.format(netDate)
            )
            smses.add(txt)
            count=count+1
        }
        return Smses(count,smses)
    }
    fun createCsv(csvData:ArrayList<Excel>,path:String){
        var str_Csv=""
        csvData.forEach {
            str_Csv=str_Csv+"${it.tranId},${it.dateRecieved!!.trim()},${it.transactionType},\"${it.details}\",\"${it.Amount}\" \n"
        }

    }
}
/*


his is tested and suitable for sd storage
*  try {
            val fileOut = FileOutputStream(excelFile)
            workbook.write(fileOut)
            fileOut.close()
            println("Here is reached 345")
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
*
*
*
*
* */