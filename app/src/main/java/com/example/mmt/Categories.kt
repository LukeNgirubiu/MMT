package com.example.mmt

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mmt.Adapter.Transactions
import com.example.mmt.Const.Project
import com.example.mmt.Utils.Dialogs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Categories : AppCompatActivity() {
    private lateinit var searchView:SearchView
    private  lateinit var recyclerView: RecyclerView
    private lateinit var txtNoData:TextView
    private var initialSearch=""
    private var newSearch=""
    private var categoryId=0
    private lateinit var transactAdapter: Transactions
    private var allTrans:ArrayList<String> = ArrayList()
    private var searched:ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)
        searchView=findViewById(R.id.search_bar)
        recyclerView=findViewById(R.id.recycleView)
        txtNoData=findViewById(R.id.txtNoData)
        categoryId=intent.getIntExtra("categoryId",0)
        searchView.clearFocus()
        welcomeDialog()
        recycler()
        searchView.setOnQueryTextListener(object :SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText!!.isBlank()){
                    recycler()
                }
                if (newText.isNotBlank()){
                    searching(newText!!.trim())
                }
                return false
            }
        })
    }
private fun searching(search:String){
        searched.clear()
        for (index in 0..fetchData().size-1){
            val items=fetchData()[index].split(",")
            if (fetchData()[index].isNotBlank()){
                if (items[0].lowercase().contains(search.lowercase())||
                    items[1].lowercase().contains(search.lowercase())||
                    items[2].lowercase().contains(search.lowercase())||
                    items[3].lowercase().contains(search.lowercase())||
                    items[4].lowercase().contains(search.lowercase())){
                    println("Matching ${items}")
                    searched.add(fetchData()[index])
                }
            }
        }
            if (searched.size>0){
                txtNoData.visibility= View.GONE
                recyclerView.visibility= View.VISIBLE
                transactAdapter= Transactions(searched,baseContext)
                recyclerView.layoutManager= LinearLayoutManager(baseContext)
                recyclerView.adapter=transactAdapter
            }
}
private fun recycler(){
    if (fetchData().size>0){
        txtNoData.visibility= View.GONE
        recyclerView.visibility= View.VISIBLE
        transactAdapter= Transactions(fetchData(),this)
        recyclerView.layoutManager= LinearLayoutManager(this)
        recyclerView.adapter=transactAdapter
    }
}
    private fun fetchData():ArrayList<String>{
        var getCsv=""
        this.openFileInput(Project.fileStore).use { stream ->
            getCsv = stream.bufferedReader().use {
                it.readText()
            }
            stream.close()
        }
        allTrans.clear()
        val dataLs=getCsv.split("\n")
        dataLs.subList(1,dataLs.size).forEach {
            if (it.isNotBlank()){
                val itemCl=it.split(",")
                when(categoryId){
                    1->{
                        if (itemCl[2]=="Received"){
                            allTrans.add(it)
                        }
                    }
                    2->{
                        if (itemCl[2]=="Sent"){
                            allTrans.add(it)
                        }
                    }
                    3->{
                        if (itemCl[2]=="Withdrew"){
                            allTrans.add(it)
                        }
                    }
                    4->{
                        if (itemCl[2]=="Paid"){
                            allTrans.add(it)
                        }
                    }
                    5->{
                        if (itemCl[2]=="Airtime Top Up"){
                            allTrans.add(it)
                        }
                    }
                }
            }
        }

      return if (allTrans.size>30) ArrayList(allTrans.subList(0,30)) else  allTrans
    }
    private fun welcomeDialog(){
        val dialog=Dialogs(this)
        when(categoryId){
            1->{
                val alert=dialog.waitingDialog("Received cash list","Last ${fetchData().size} transactions ")
                Handler().postDelayed({
                    alert.cancel()
                },3000)
            }
            2->{
                val alert=dialog.waitingDialog("Sent cash list","Last ${fetchData().size} transactions ")
                Handler().postDelayed({
                    alert.cancel()
                },3000)
            }
            3->{
                val alert=dialog.waitingDialog("Withdrew cash list","Last ${fetchData().size} transactions ")
                Handler().postDelayed({
                    alert.cancel()
                },3000)
            }
            4->{
                val alert=dialog.waitingDialog("Paid cash list","Last ${fetchData().size} transactions ")
                Handler().postDelayed({
                    alert.cancel()
                },3000)
            }
            5->{
                val alert=dialog.waitingDialog("Airtime top Up cash list","Last ${fetchData().size} transactions ")
                Handler().postDelayed({
                    alert.cancel()
                },3000)
            }
        }
    }
}
