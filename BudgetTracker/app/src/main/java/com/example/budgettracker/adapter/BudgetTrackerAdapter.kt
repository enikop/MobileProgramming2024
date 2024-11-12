package com.example.budgettracker.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettracker.MainActivity
import com.example.budgettracker.R
import com.example.budgettracker.database.AppDatabase
import com.example.budgettracker.fragments.HomeFragment
import com.example.budgettracker.helpers.DateConverter
import com.example.budgettracker.model.Transaction
import com.example.budgettracker.touch.TransactionTouchHelperAdapter
import kotlinx.android.synthetic.main.row_item.view.*
import java.text.DecimalFormat
import java.util.*

class BudgetTrackerAdapter : RecyclerView.Adapter<BudgetTrackerAdapter.ViewHolder>, TransactionTouchHelperAdapter {
    private val items = mutableListOf<Transaction>()
    private val dateConverter = DateConverter()
    private val context: Context
    private val fragment: HomeFragment

    constructor(context: Context, items: List<Transaction>, fragment: HomeFragment,) : super() {
        this.context = context
        this.fragment = fragment
        this.items.addAll(items)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.row_item, parent, false
        )
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val decimalFormat = DecimalFormat("#.##")
        /*Itt kérjük le az egyes ShoppingItem elemek adattagjait, itt is szükséges az adattaggal a bővítés*/
        holder.tvLabel.text = items[position].label
        holder.tvAmount.text = decimalFormat.format(items[position].amount)
        holder.tvCurrency.text = items[position].currency.name
        holder.cbComplete.isChecked = items[position].isCompleted
        holder.tvNote.text = items[position].note
        holder.tvDate.text = dateConverter.dateToString(items[position].date)

        val backgroundColor = if (items[position].isIncoming) {
            ContextCompat.getColor(holder.itemView.context, R.color.incomingColour)
        } else {
            ContextCompat.getColor(holder.itemView.context, R.color.outgoingColour)
        }

        holder.cardView.setCardBackgroundColor(backgroundColor)
        /*Delete gomb eseménykezeője (a főoldalon)*/
        holder.btnDelete.setOnClickListener {
            deleteItem(holder.adapterPosition)
        }
        /*Edit gomb eseménykezelője (a főoldalon), megnyitja az edit dialógust, átadja az adott ShoppingItem-et neki*/
        holder.btnEdit.setOnClickListener {
            fragment.showEditItemDialog(
                items[holder.adapterPosition])
        }
        /*Checkbox eseménykezelője, állítja a checkbox értékét, azaz a ShoppingItem-nek, az isChecked adattagját.
        Az adatbázisban is frissíti
         */
        holder.cbComplete.setOnClickListener {
            items[position].isCompleted = holder.cbComplete.isChecked
            val dbThread = Thread {
                //Itt frissíti a DB-ben
                AppDatabase.getInstance(context).transactionDao().updateItem(items[position])
            }
            dbThread.start()
        }
    }
    /*Új elem hozzáadásakor hívódik meg*/
    fun addItem(item: Transaction) {
        items.add(item)
        notifyItemInserted(items.lastIndex)
    }
    /*Elem törlésekor hívódik meg. Az adatbázisból törli az elemet (DAO-n keresztül)*/
    fun deleteItem(position: Int) {
        val dbThread = Thread {
            AppDatabase.getInstance(context).transactionDao().deleteItem(
                items[position])
            (context as MainActivity).runOnUiThread{
                items.removeAt(position)
                notifyItemRemoved(position)
            }
        }
        dbThread.start()
    }
    /*Update-kor hívódik meg*/
    fun updateItem(item: Transaction) {
        val idx = items.indexOf(item)
        items[idx] = item
        notifyItemChanged(idx)
    }

    override fun onItemDismissed(position: Int) {
        deleteItem(position)
    }

    override fun onItemMoved(fromPosition: Int, toPosition: Int) {
        Collections.swap(items, fromPosition, toPosition)

        notifyItemMoved(fromPosition, toPosition)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        /*a ShoppingItem elemek, ide kell a bővítés új taggal*/
        /*Itt a gombokat, checkboxot is lekérjük*/
        val tvLabel: TextView = itemView.tvLabel
        val tvAmount: TextView = itemView.tvAmount
        val tvCurrency: TextView = itemView.tvCurrency
        val cbComplete: CheckBox = itemView.cbComplete
        val tvNote: TextView = itemView.tvNote
        val tvDate: TextView = itemView.tvDate

        val btnDelete: Button = itemView.btnDelete
        val btnEdit: Button = itemView.btnEdit
        val cardView: CardView = itemView.card_view
    }
}