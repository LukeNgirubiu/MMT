package com.example.mmt
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mmt.Adapter.Transactions
import com.example.mmt.Const.Project
import com.example.mmt.Model.Mpesa
import com.example.mmt.Utils.Dialogs
import com.example.mmt.Utils.mpesaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
      private lateinit var recievedAmount: TextView
      private lateinit var sentAmount:TextView
      private lateinit var withdrawAmount:TextView
      private lateinit var paidAmount:TextView
      private lateinit var airtimeAmount:TextView
      private lateinit var balanceChecks:TextView
      private lateinit var mpesaData:Mpesa
      private lateinit var sharedPreferences: SharedPreferences
      private lateinit var mpesaUt:mpesaUtils
      private lateinit var allTexts:List<String>
      private lateinit var recycleView:RecyclerView
      private lateinit var noText:TextView
      private lateinit var transactAdapter: Transactions
      private lateinit var mainMenu:ImageButton
      private lateinit var recievedCash:CardView
      private lateinit var sentCash:CardView
      private lateinit var withdrawnCash:CardView
      private lateinit var paidCash:CardView
      private lateinit var airTimeCash:CardView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recievedAmount=findViewById(R.id.recievedAmount)
        sentAmount=findViewById(R.id.sentAmount)
        withdrawAmount=findViewById(R.id.withdrawAmount)
        paidAmount=findViewById(R.id.paidAmount)
        airtimeAmount=findViewById(R.id.airtimeAmount)
        balanceChecks=findViewById(R.id.balanceChecks)
        recycleView=findViewById(R.id.recycleView)
        mainMenu=findViewById(R.id.main_menu)
        noText=findViewById(R.id.noText)
        recievedCash=findViewById(R.id.recieved)
        sentCash=findViewById(R.id.sent)
        withdrawnCash=findViewById(R.id.withdraw)
        paidCash=findViewById(R.id.paid)
        airTimeCash=findViewById(R.id.airTime)
        sharedPreferences=getSharedPreferences("App", Context.MODE_PRIVATE)
        mpesaUt= mpesaUtils(this)
        mainMenu.setOnClickListener {
            popUpMenu()
        }
        val toCategory=Intent(this,Categories::class.java)
        recievedCash.setOnClickListener {
            toCategory.putExtra("categoryId",1)
            startActivity(toCategory)
        }
        sentCash.setOnClickListener {
            toCategory.putExtra("categoryId",2)
            startActivity(toCategory)
        }
        withdrawnCash.setOnClickListener {
            toCategory.putExtra("categoryId",3)
            startActivity(toCategory)
        }
        paidCash.setOnClickListener {
            toCategory.putExtra("categoryId",4)
            startActivity(toCategory)
        }
        airTimeCash.setOnClickListener {
            toCategory.putExtra("categoryId",5)
            startActivity(toCategory)
        }


    }


    override fun onResume() {
        super.onResume()
        if (checkPermissions()==false){
            mainMenu.visibility=View.GONE
            requestPermission()
        }
        if (checkPermissions()==true){
            mainMenu.visibility=View.VISIBLE
            prepareData()
            if (allTexts.size>0){
                noText.visibility=View.GONE
                recycleView.visibility=View.VISIBLE
                transactAdapter=Transactions(ArrayList(allTexts.subList(1,6)),this)
                recycleView.layoutManager=LinearLayoutManager(this)
                recycleView.adapter=transactAdapter
            }
        }
    }
    fun prepareData(){
        val started=sharedPreferences.getBoolean("Started",false)
        if (started==false){
            mpesaData=mpesaUt.dataUtil(mpesaUt.smsesUtil().mpesaSmses)
            recievedAmount.text="Ksh ${mpesaData.sumTotals[2]}"
            sentAmount.text="Ksh ${mpesaData.sumTotals[0]}"
            withdrawAmount.text="Ksh ${mpesaData.sumTotals[4]}"
            paidAmount.text="Ksh ${mpesaData.sumTotals[3]}"
            airtimeAmount.text="Ksh ${mpesaData.sumTotals[1]}"
            balanceChecks.text="${mpesaData.sumTotals[5]}"
            var csvSt=mpesaData.sumTotals.joinToString(",")
            csvSt=csvSt+",${mpesaData.smsData.size}\n"
            mpesaData.smsData.forEach {
                csvSt=csvSt+"${it.tranId},${it.dateRecieved!!.trim()},${it.transactionType},${it.details},${it.Amount} \n"
            }
            allTexts=csvSt.split("\n")
            val absoluteP=filesDir.absolutePath+"/${Project.fileStore}"
            val fw = FileWriter(absoluteP)
            fw.write(csvSt)
            fw.close()
            sharedPreferences.edit().putBoolean("Started",true).commit()
        }
        if(started==true){
            var getCsv=""
            this.openFileInput(Project.fileStore).use { stream ->
                getCsv = stream.bufferedReader().use {
                    it.readText()
                }
                stream.close()
            }
            val dataLs=getCsv.split("\n")
            val smesNum=mpesaUt.dataUtil(mpesaUt.smsesUtil().mpesaSmses)
            val summary=dataLs[0].split(",")
            var csvSmsNum=summary.last().trim().toDouble().toInt()
            var headerCl:ArrayList<Double> = ArrayList()
            headerCl.addAll(listOf(summary[0].trim().toDouble(),summary[1].trim().toDouble(),summary[2].trim().toDouble(),summary[3].trim().toDouble(),summary[4].trim().toDouble(),summary[5].trim().toDouble()))
            if (smesNum.smsData.size!=csvSmsNum){
                var additionStr=""
                var mpesaIdCsv=ArrayList<String>()
                for (x in 1..dataLs.size-1){
                mpesaIdCsv.add(dataLs[x].split(",").first().trim())
                }
                var nums=0
                for (sms in smesNum.smsData){
                    if(!mpesaIdCsv.contains(sms.tranId)){
                        when(sms.transactionType){
                            "Sent"->{
                                headerCl[0]=headerCl[0]+sms.Amount
                            }
                            "Airtime Top Up"->{
                                headerCl[1]=headerCl[1]+sms.Amount
                            }
                            "Received"->{
                                headerCl[2]=headerCl[2]+sms.Amount
                            }
                            "Paid"->{
                                headerCl[3]=headerCl[3]+sms.Amount
                            }
                            "Withdrew"->{
                                headerCl[4]=headerCl[4]+sms.Amount
                            }
                            "Balance"->{
                                headerCl[5]=headerCl[5]+1
                            }
                        }
                        additionStr=additionStr+"${sms.tranId},${sms.dateRecieved!!.trim()},${sms.transactionType},${sms.details},${sms.Amount} \n"
                        nums=nums+1
                    }
                }
                val firstColumn=headerCl.subList(0,headerCl.lastIndex+1).joinToString(",")+",${summary.last().trim().toDouble()+nums}\n"
                val formerColums=dataLs.subList(1,dataLs.lastIndex+1).joinToString("\n")
                val allData=firstColumn+additionStr+formerColums
                deleteFile(Project.fileStore)
                this.openFileOutput(Project.fileStore, Context.MODE_PRIVATE).use { output ->
                    output.write(allData.toByteArray())
                    output.close()
                }
                recievedAmount.text="Ksh ${headerCl[2]}"
                sentAmount.text="Ksh ${headerCl[0]}"
                withdrawAmount.text="Ksh ${headerCl[4]}"
                paidAmount.text="Ksh ${headerCl[3]}"
                airtimeAmount.text="Ksh ${headerCl[1]}"
                balanceChecks.text="${headerCl[5].toInt()} Times"
                allTexts=allData.split("\n")
            }
            if (smesNum.smsData.size==csvSmsNum){
            recievedAmount.text="Ksh ${summary[2]}"
            sentAmount.text="Ksh ${summary[0]}"
            withdrawAmount.text="Ksh ${summary[4]}"
            paidAmount.text="Ksh ${summary[3]}"
            airtimeAmount.text="Ksh ${summary[1]}"
            balanceChecks.text="${summary[5].replace(".0","")} Times"
            allTexts=getCsv.split("\n")
            }
        }
    }
 private fun popUpMenu(){
     val popup= PopupMenu(Home@this,mainMenu)
     popup.inflate(R.menu.main)
     popup.setOnMenuItemClickListener { item: MenuItem ->
         when(item.itemId){
             R.id.share_csv->{
                 createCsv(1)
             }
             R.id.share_excel->{
                 createExcel()
             }
             R.id.see_csv->{
                 createCsv(2)
             }
         }
         true
     }
     popup.show()
 }
private fun createCsv(action:Int){
    var csvStr=""
    lifecycleScope.launch(Dispatchers.Default){
        Project.columns.forEachIndexed{ index,element->
            if (index==Project.columns.size-1){
                csvStr=csvStr+"\"$element\""
            }
            else{
                csvStr=csvStr+"\"$element\","
            }
        }
        csvStr=csvStr+"\n"
        allTexts.subList(1,allTexts.size).forEach {
            if (it.isNotBlank()){
                val singleRow=it.split(",")
                if (singleRow[0].isNotBlank()&&singleRow[1].isNotBlank()&&singleRow[2].isNotBlank()&&singleRow[3].isNotBlank()&&singleRow[4].isNotBlank()){
                    csvStr=csvStr+"\"${singleRow[0]}\",${singleRow[1]},\"${singleRow[2]}\",\"${singleRow[3]}\",${singleRow[4]}\n"
                }
            }
        }
        baseContext.openFileOutput(Project.shareCsv, Context.MODE_PRIVATE).use { output ->
            output.write(csvStr.toByteArray())
            output.close()
        }
        shareFile(1,action)
    }
}
private fun createExcel(){
val dialog=Dialogs(this)
val alert=dialog.waitingDialog("Preparing excel","Please wait ...")
lifecycleScope.launch(Dispatchers.Default){
    val workbook: Workbook =  XSSFWorkbook()
    var cell: Cell? =null
    var sheet: Sheet = workbook.createSheet("mpesa")
    val row: Row = sheet.createRow(0)
    Project.columns.forEachIndexed{ index,element->
        cell = row.createCell(index);
        cell?.setCellValue(element);
    }
    allTexts.subList(1,allTexts.size).forEachIndexed{index,element->
        if (element.isNotBlank()){
            val item=element.split(",")
            if (item[0].isNotBlank()&&item[1].isNotBlank()&&item[2].isNotBlank()&&item[3].isNotBlank()&&item[4].isNotBlank()){
                val rowData = sheet.createRow(index + 1)
                cell = rowData.createCell(0)
                cell?.setCellValue(item[0])
                cell = rowData.createCell(1)
                cell?.setCellValue(item[1])
                cell = rowData.createCell(2)
                cell?.setCellValue(item[2])
                cell = rowData.createCell(3)
                cell?.setCellValue(item[3])
                cell = rowData.createCell(4)
                cell?.setCellValue(item[4])
            }
        }
    }
    val excelFile = File(filesDir.absolutePath, "${Project.shareExcel}")
    var status=false
    try {
        val fileOut = FileOutputStream(excelFile)
        workbook.write(fileOut)
        fileOut.close()
        status=true
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }
    if (status==false){
        withContext(Dispatchers.Main){
          alert.cancel()
          Toast.makeText(baseContext,"Failed to get the file",Toast.LENGTH_LONG).show()
        }
    }
    if (status==true){
        withContext(Dispatchers.Main){
            alert.cancel()
            shareFile(2,1)
        }
    }
}
}
private fun shareFile(fileType:Int,action:Int){
    val fileToShare=if(fileType==1) Project.shareCsv else Project.shareExcel
    val actionView=if(action==1) Intent.ACTION_SEND else Intent.ACTION_VIEW
    val file= File(filesDir.absolutePath+"/$fileToShare")
    val uriFile=if (Build.VERSION.SDK_INT<Build.VERSION_CODES.N) Uri.fromFile(file) else FileProvider.getUriForFile(applicationContext,BuildConfig.APPLICATION_ID+".provider",file)
    val share= Intent()
    share.setAction(actionView)
    if (action==2){
        share.setDataAndType (uriFile, "*/*");
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    if (action==1){
        share.putExtra(Intent.EXTRA_STREAM, uriFile)
        share.setType("*/*")
    }
    startActivity(share)
}
    companion object{
        private const val SMS_PERMISSIONS=100
    }
    private fun checkPermissions():Boolean{
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.RECEIVE_SMS)== PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_SMS)== PackageManager.PERMISSION_GRANTED ){
            return true
        }
        return false
    }
    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS),
            SMS_PERMISSIONS)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode== SMS_PERMISSIONS){
            if(grantResults.isEmpty() && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Permissions granted",Toast.LENGTH_LONG).show()
            }
        }
    }

}
/*

 val where = filesDir.absolutePath

    val excelFile = File(filesDir.absolutePath, "${Project.shareExcel}")
    try {
        val fileOut = FileOutputStream(excelFile)
        workbook.write(fileOut)
        fileOut.close()
        println("Here is reached 345")
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }



* */

