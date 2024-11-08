package com.example.budgettracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.budgettracker.adapter.BudgetTrackerAdapter
import com.example.budgettracker.database.AppDatabase
import com.example.budgettracker.dialog.TransactionDialog
import com.example.budgettracker.fragments.MainFragment
import com.example.budgettracker.fragments.StatFragment
import com.example.budgettracker.model.Transaction
import com.example.budgettracker.touch.TransactionTouchHelperCallback
import kotlinx.android.synthetic.main.activity_main.*
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

class MainActivity : AppCompatActivity(), TransactionDialog.TransactionHandler {
    companion object {
        val KEY_FIRST = "KEY_FIRST"
        val KEY_ITEM_TO_EDIT = "KEY_ITEM_TO_EDIT"
    }
    private inner class ScreenSlidePagerAdapter(activity: AppCompatActivity, adapter:BudgetTrackerAdapter) :
        FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 2 // Two fragments
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> MainFragment(adapter) // Your original MainFragment
                else -> StatFragment() // A second empty fragment
            }
        }
    }

    private lateinit var adapter: BudgetTrackerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        //Új elem hozzáadásakor hívódik meg, a rózsaszín levél ikonos gomb eseménykezelője
        //A ShoppingTimeDialog-ot hívja meg (jeleníti meg)
        fab.setOnClickListener { view ->
            TransactionDialog().show(supportFragmentManager, "TAG_ITEM")
        }
        initRecyclerView()

        /*Új elem felvitelének vizsgálata, akkor hívódik meg, a dialógus címét állítja*/
        if (isFirstRun()) {
            MaterialTapTargetPrompt.Builder(this@MainActivity)
                .setTarget(findViewById<View>(R.id.fab))
                .setPrimaryText("New transaction")
                .setSecondaryText("Tap here to create a new transaction")
                .show()
        }


        saveThatItWasStarted()
    }

    private fun isFirstRun(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
            KEY_FIRST, true
        )
    }

    private fun saveThatItWasStarted() {
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        sp.edit()
            .putBoolean(KEY_FIRST, false)
            .apply()
    }

    private fun initRecyclerView() {
        val dbThread = Thread {
            //Lekéri az összes Shopping Item-et.
            val items = AppDatabase.getInstance(this).transactionDao().findAllItems()

            runOnUiThread{
                adapter = BudgetTrackerAdapter(this, items)
                val viewPager: ViewPager2 = viewPager
                val pagerAdapter = ScreenSlidePagerAdapter(this, adapter)
                viewPager.adapter = pagerAdapter
                /*recyclerShopping.adapter = adapter

                val callback = TransactionTouchHelperCallback(adapter)
                val touchHelper = ItemTouchHelper(callback)
                touchHelper.attachToRecyclerView(recyclerShopping)*/
            }
        }
        dbThread.start()
    }

    /*Edit dialógus megnyitása*/
    fun showEditItemDialog(itemToEdit: Transaction) {
        val editDialog = TransactionDialog()

        val bundle = Bundle()
        bundle.putSerializable(KEY_ITEM_TO_EDIT, itemToEdit)
        editDialog.arguments = bundle

        editDialog.show(supportFragmentManager, "TAG_ITEM_EDIT")
    }

    /*Új Shopping Item-kor beszúrjuk a DB-be a DAO segítségével*/
    override fun transactionCreated(item: Transaction) {
        val dbThread = Thread {
            val id = AppDatabase.getInstance(this@MainActivity).transactionDao().insertItem(item)

            item.id = id

            runOnUiThread{
                adapter.addItem(item)
            }
        }
        dbThread.start()
    }
    /*Update-or módosítjuk a Shopping Item-et a DAO segítségével*/
    override fun transactionUpdated(item: Transaction) {
        adapter.updateItem(item)

        val dbThread = Thread {
            AppDatabase.getInstance(this@MainActivity).transactionDao().updateItem(item)

            runOnUiThread { adapter.updateItem(item) }
        }
        dbThread.start()
    }

}