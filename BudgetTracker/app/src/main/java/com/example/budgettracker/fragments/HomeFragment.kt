package com.example.budgettracker.fragments

import android.app.Activity
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.budgettracker.MainActivity
import com.example.budgettracker.R
import com.example.budgettracker.adapter.BudgetTrackerAdapter
import com.example.budgettracker.database.AppDatabase
import com.example.budgettracker.dialog.TransactionDialog
import com.example.budgettracker.model.Transaction
import com.example.budgettracker.touch.TransactionTouchHelperCallback
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

class HomeFragment : Fragment(), TransactionDialog.TransactionHandler {
    private lateinit var adapter: BudgetTrackerAdapter
    private lateinit var activity: Activity

    companion object {
        const val KEY_FIRST = "KEY_FIRST"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("HomeFragment", "HomeFragment is created")
        view.fab.setOnClickListener {
            TransactionDialog(this).show(parentFragmentManager, "TAG_ITEM")
        }

        initRecyclerView()
        activity = requireActivity()
        if (isFirstRun()) {
            MaterialTapTargetPrompt.Builder(activity)
                .setTarget(view.fab)
                .setPrimaryText("New Transaction")
                .setSecondaryText("Tap here to record a new transaction.")
                .show()
        }

        saveThatItWasStarted()
    }

    private fun isFirstRun(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean(KEY_FIRST, true)
    }

    private fun saveThatItWasStarted() {
        val sp = PreferenceManager.getDefaultSharedPreferences(requireContext())
        sp.edit().putBoolean(KEY_FIRST, false).apply()
    }

    private fun initRecyclerView() {
        val dbThread = Thread {
            val items = AppDatabase.getInstance(requireContext()).transactionDao().findAllItems()

            activity.runOnUiThread {
                adapter = BudgetTrackerAdapter(requireContext(), items, this)
                rvTransaction.adapter = adapter
                val callback = TransactionTouchHelperCallback(adapter)
                val touchHelper = ItemTouchHelper(callback)
                touchHelper.attachToRecyclerView(view?.rvTransaction)
            }
        }
        dbThread.start()
    }

    override fun transactionCreated(item: Transaction) {
        val dbThread = Thread {
            val id = AppDatabase.getInstance(requireContext()).transactionDao().insertItem(item)
            item.id = id

            activity.runOnUiThread {
                adapter.addItem(item)
            }
        }
        dbThread.start()
    }

    override fun transactionUpdated(item: Transaction) {
        adapter.updateItem(item)

        val dbThread = Thread {
            AppDatabase.getInstance(requireContext()).transactionDao().updateItem(item)
            activity.runOnUiThread { adapter.updateItem(item) }
        }
        dbThread.start()
    }
    fun showEditItemDialog(itemToEdit: Transaction) {
        val editDialog = TransactionDialog(this)

        val bundle = Bundle()
        bundle.putSerializable(MainActivity.KEY_ITEM_TO_EDIT, itemToEdit)
        editDialog.arguments = bundle

        editDialog.show(parentFragmentManager, "TAG_ITEM_EDIT")
    }
}