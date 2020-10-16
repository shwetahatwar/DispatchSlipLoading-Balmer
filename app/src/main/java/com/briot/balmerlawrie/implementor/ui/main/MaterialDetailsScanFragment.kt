package com.briot.balmerlawrie.implementor.ui.main

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.briot.balmerlawrie.implementor.R
import com.briot.balmerlawrie.implementor.UiHelper
import com.briot.balmerlawrie.implementor.repository.remote.DispatchSlip
import com.briot.balmerlawrie.implementor.repository.remote.MaterialInward
import com.pascalwelsch.arrayadapter.ArrayAdapter
import io.github.pierry.progress.Progress
import kotlinx.android.synthetic.main.material_details_row.view.*
import kotlinx.android.synthetic.main.material_details_scan_fragment.*
import java.security.Timestamp
import java.text.SimpleDateFormat
import java.util.*


class MaterialDetailsScanFragment : Fragment() {

    companion object {
        fun newInstance() = MaterialDetailsScanFragment()
    }

    private lateinit var viewModel: MaterialDetailsScanViewModel
    private var progress: Progress? = null
    private var oldMaterialInward: MaterialInward? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.material_details_scan_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MaterialDetailsScanViewModel::class.java)

        (this.activity as AppCompatActivity).setTitle("Material Details")

        materialScanText.requestFocus()

        materialItemsList.adapter = MaterialItemsAdapter(this.context!!)
        materialItemsList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this.context)

        val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view!!.windowToken, 0)
//        materialResultId.visibility = View.GONE

        viewModel.materialInwards.observe(this, Observer<MaterialInward> {
            UiHelper.hideProgress(this.progress)
            this.progress = null

//            materialResultId.visibility = View.GONE
            (materialItemsList.adapter as MaterialItemsAdapter).clear()
            if (it != null && it != oldMaterialInward) {
                materialScanText.text?.clear()
                materialScanText.requestFocus()

                (materialItemsList.adapter as MaterialItemsAdapter).add(it)

                // dismiss keyboard now
                if (activity != null) {
                    val keyboard = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    keyboard.hideSoftInputFromWindow(activity?.currentFocus?.getWindowToken(), 0)
                }

                if (it.dispatchSlip == null)  {
                    UiHelper.hideProgress(this.progress)
                    this.progress = null
                    (materialItemsList.adapter as MaterialItemsAdapter).notifyDataSetChanged()
                }  else  {
                    viewModel.getMaterialDispatchSlip(it.dispatchSlip!!.dispatchSlipNumber)
                }
            }

            oldMaterialInward = it

            if (it == null) {
                UiHelper.showErrorToast(this.activity as AppCompatActivity, "Material not found for scanned Barcode")
                materialScanText.text?.clear()
                materialScanText.requestFocus()
            }
        })

        viewModel.networkError.observe(this, Observer<Boolean> {
            if (it == true) {
                UiHelper.hideProgress(this.progress)
                this.progress = null

                UiHelper.showAlert(this.activity as AppCompatActivity, "Server is not reachable, please check if your network connection is working");
            }
        })

        viewModel.dispatchSlip.observe(this, Observer<DispatchSlip> {
            if (it != null) {
                (this.materialItemsList.adapter as MaterialItemsAdapter).getItem(0)?.dispatchSlip = it
            }

            (materialItemsList.adapter as MaterialItemsAdapter).notifyDataSetChanged()
        })

        materialScanText.setOnEditorActionListener { _, i, keyEvent ->
            var handled = false
            if (keyEvent == null) {
                Log.d("materialDetailsScan: ", "event is null")
            } else if ((materialScanText.text != null && materialScanText.text!!.isNotEmpty()) && i == EditorInfo.IME_ACTION_DONE || ((keyEvent.keyCode == KeyEvent.KEYCODE_ENTER || keyEvent.keyCode == KeyEvent.KEYCODE_TAB) && keyEvent.action == KeyEvent.ACTION_DOWN)) {
                this.progress = UiHelper.showProgressIndicator(this.activity as AppCompatActivity, "Please wait")
//                materialResultId.removeAllViews()
                (materialItemsList.adapter as MaterialItemsAdapter).clear()

                val value = materialScanText.text.toString().trim()
                var arguments  = value.split("#")
                var barcodeToLoad = arguments[0]+"#"+arguments[1]+"#"+arguments[2]
                viewModel.loadMaterialItems(barcodeToLoad)

                materialScanText.requestFocus()
                materialScanText.text?.clear()
                handled = true
            }
            handled
        }

        viewMaterialDetails.setOnClickListener {
            if (materialScanText.text != null && materialScanText.text!!.isNotEmpty()) {
                this.progress = UiHelper.showProgressIndicator(this.activity as AppCompatActivity, "Please wait")
//                materialResultId.removeAllViews()

                (materialItemsList.adapter as MaterialItemsAdapter).clear()
                val value = materialScanText.text.toString().trim()
                var arguments  = value.split("#")
                if (arguments.size < 3 || arguments[0].length == 0 || arguments[1].length == 0 || arguments[2].length == 0) {
                    UiHelper.showErrorToast(this.activity as AppCompatActivity, "Invalid barcode, please try again!")
                    UiHelper.hideProgress(progress);
                }else{
                    var barcodeToLoad = arguments[0]+"#"+arguments[1]+"#"+arguments[2]
                    viewModel.loadMaterialItems(barcodeToLoad)
                }
            }else{
                UiHelper.showErrorToast(this.activity as AppCompatActivity, "Enter Barcode Material!")
            }
        }
    }

}

class MaterialItemsAdapter(val context: Context) : ArrayAdapter<MaterialInward, MaterialItemsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        val status: TextView
        val materialBarcode: TextView
        val materialGenericName: TextView
        val materialDescription: TextView
        val inwardedOn: TextView
        val inwardedBy: TextView
        val scrappedOn: TextView
        val scrappedBy: TextView
        val recoveredOn: TextView
        val recoveredBy: TextView
        val pickedOn: TextView
        val pickedBy: TextView
        val loadedOn: TextView
        val loadedBy: TextView
        val materialDispatchSlipNumber: TextView
        val materialDispatchTruckNumber: TextView
        val depot: TextView


        init {
            status = itemView.material_scrap_value as TextView
            materialBarcode = itemView.material_serialnumber_value as TextView
            materialGenericName = itemView.material_generic_name_value as TextView
            materialDescription = itemView.material_description_value as TextView
            inwardedOn = itemView.inwarded_on_value as TextView
            inwardedBy = itemView.inwarded_by_value as TextView
            scrappedOn = itemView.scrapped_on_value as TextView
            scrappedBy = itemView.scrapped_by_value as TextView
            recoveredOn = itemView.recovered_on_value as TextView
            recoveredBy = itemView.recovered_by_value as TextView
            pickedOn = itemView.picked_on_value as TextView
            pickedBy = itemView.picked_by_value as TextView
            loadedOn = itemView.loaded_on_value as TextView
            loadedBy = itemView.loaded_by_value as TextView
            materialDispatchSlipNumber = itemView.dispatch_slip_value as TextView
            materialDispatchTruckNumber = itemView.material_trucknumber_value as TextView
            depot = itemView.material_depot_value as TextView


//            materialLoader = itemView.material_loader_value as TextView
//            materialDispatchTruckNumber = itemView.material_trucknumber_value as TextView
//            depot = itemView.material_depot_value as TextView
//            materialScrapped = itemView.material_scrap_value as TextView
        }
    }

    override fun getItemId(item: MaterialInward): Any {
        return item
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = getItem(position) as MaterialInward
        holder.status.text = item.status
        holder.materialBarcode.text = item.serialNumber
        holder.materialGenericName.text = item.materialGenericName
        holder.materialDescription.text = item.materialDescription

        if (item.inwardedOn != null && item.inwardedOn != "NA"){
            holder.inwardedOn.text = item.inwardedOn?.let { timeConverter(it.toLong()) }
        }else{
            item.inwardedOn = "NA"
        }

        holder.inwardedBy.text = item.inwardedBy

        if (item.scrappedOn != null && item.scrappedOn != "NA"){
            holder.scrappedOn.text = item.scrappedOn?.let { timeConverter(it.toLong()) }
        }else{
            item.scrappedOn = "NA"
        }


        holder.scrappedBy.text = item.scrappedBy


        if (item.recoveredOn != null && item.recoveredOn != "NA"){
            holder.recoveredOn.text = item.recoveredOn?.let { timeConverter(it.toLong()) }
        }else{
            item.recoveredOn = "NA"
        }

        holder.recoveredBy.text = item.recoveredBy


        if (item.pickedOn != null && item.pickedOn != "NA"){
            holder.pickedOn.text = item.pickedOn?.let { timeConverter(it.toLong()) }
        }else{
            item.pickedOn = "NA"
        }

        holder.pickedBy.text = item.pickedBy

        if (item.loadedOn != null && item.loadedOn != "NA"){
            holder.loadedOn.text = item.loadedOn?.let { timeConverter(it.toLong()) }
        }else{
            item.loadedOn = "NA"
        }

        holder.loadedBy.text = item.loadedBy
        if (item.dispatchSlip != null) {
            holder.materialDispatchSlipNumber.text = item.dispatchSlip!!.dispatchSlipNumber
        }
        if(item.ttat != null){
            holder.materialDispatchTruckNumber.text = item.ttat!!.truckNumber
        }
        if(item.depot != null){
            holder.depot.text = item.depot!!.name
        }
    }

    fun timeConverter(s: Long): String? {
        val date1 = SimpleDateFormat("dd/MM/yyyy hh:mm:ss a").format(Date(s))
        println("date-->"+date1)
        return date1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
                .inflate(R.layout.material_details_row, parent, false)
        return ViewHolder(view)
    }
}
