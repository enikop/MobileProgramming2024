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
import com.example.budgettracker.dialog.TransactionHandler
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
    private val handler: TransactionHandler

    constructor(context: Context, items: List<Transaction>, handler: TransactionHandler) : super() {
        this.context = context
        this.handler = handler
        this.items.addAll(items)
        this.items.sortByDescending { it.date }

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

        holder.btnDelete.setOnClickListener {
            deleteItem(holder.adapterPosition)
        }

        holder.btnEdit.setOnClickListener {
            handler.showEditItemDialog(
                items[holder.adapterPosition])
        }

        holder.cbComplete.setOnClickListener {
            items[holder.adapterPosition].isCompleted = holder.cbComplete.isChecked
            val dbThread = Thread {
                AppDatabase.getInstance(context).transactionDao().updateItem(items[holder.adapterPosition])
            }
            dbThread.start()
        }
    }

    fun addItem(item: Transaction) {
        items.add(0, item)
        notifyItemInserted(0)
    }

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