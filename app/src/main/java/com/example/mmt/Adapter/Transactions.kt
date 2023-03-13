package com.example.mmt.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mmt.R

class Transactions(internal val items:ArrayList<String>,internal val context:Context):RecyclerView.Adapter<Transactions.viewTransaction>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewTransaction {
        val layout=LayoutInflater.from(context).inflate(R.layout.single_items_mpesa,parent,false)
        return viewTransaction(layout)
    }
    override fun onBindViewHolder(holder: viewTransaction, position: Int) {
        val item=items.get(position).split(",")
        holder.mpesaId.text=item[0].trim()
        holder.tDate.text=item[1].trim()
        holder.transactionType.text=item[2].trim()
        holder.Details.text=item[3].trim()
        holder.amount.text="Ksh. "+item[4].trim()
    }

    override fun getItemCount(): Int {
        return items.size
    }
    inner class viewTransaction(view: View):RecyclerView.ViewHolder(view) {
        val mpesaId=view.findViewById<TextView>(R.id.mpesaId)
        val tDate=view.findViewById<TextView>(R.id.tDate)
        val transactionType=view.findViewById<TextView>(R.id.trancType)
        val Details=view.findViewById<TextView>(R.id.details)
        val amount=view.findViewById<TextView>(R.id.amount)
    }
}