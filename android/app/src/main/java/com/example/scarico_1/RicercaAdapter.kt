package com.example.scarico_1

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.LinearLayout
import android.widget.TextView
import java.util.*

class RicercaAdapter(context: Context, private var itemsCompleti: List<Articolo>) :
    ArrayAdapter<Articolo>(context, 0, itemsCompleti) {

    private var itemsFiltrati: List<Articolo> = itemsCompleti

    private class ViewHolder(val txtDesc: TextView, val txtGiac: TextView)

    fun updateList(nuevaLista: List<Articolo>) {
        this.itemsCompleti = nuevaLista
        this.itemsFiltrati = nuevaLista
        notifyDataSetChanged()
    }

    override fun getCount(): Int = itemsFiltrati.size
    override fun getItem(position: Int): Articolo = itemsFiltrati[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layout: LinearLayout
        val holder: ViewHolder
        val density = context.resources.displayMetrics.density

        if (convertView != null && convertView.tag is ViewHolder) {
            layout = convertView as LinearLayout
            holder = convertView.tag as ViewHolder
        } else {
            layout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(Color.WHITE)
                val padH = (16 * density).toInt()
                val padV = (14 * density).toInt()
                setPadding(padH, padV, padH, padV)
                // CORRETTO: in LinearLayout si usa minimumHeight
                minimumHeight = (64 * density).toInt()
            }

            val tvDesc = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setTextColor(Color.BLACK)
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }

            val tvGiac = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins((12 * density).toInt(), 0, 0, 0)
                }
                textSize = 21f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.END
            }

            layout.addView(tvDesc)
            layout.addView(tvGiac)

            holder = ViewHolder(tvDesc, tvGiac)
            layout.tag = holder
        }

        val art = getItem(position)
        holder.txtDesc.text = "${art.codice} - ${art.descrizione}"

        if (art.lotti.isEmpty() && art.categoria.isNullOrEmpty()) {
            val dispEffettiva = art.giacenzaReale - GestoreCarrello.getQta(art.codice)
            val dispArrotondata = Math.round(dispEffettiva * 10.0) / 10.0

            val testoGiac = String.format(Locale.US, "%.1f", dispArrotondata)
            holder.txtGiac.text = testoGiac

            if (dispArrotondata > 0) {
                holder.txtGiac.setTextColor(Color.parseColor("#2E7D32"))
            } else {
                holder.txtGiac.setTextColor(Color.parseColor("#D32F2F"))
            }
            holder.txtGiac.visibility = View.VISIBLE
        } else {
            holder.txtGiac.text = ""
            holder.txtGiac.visibility = View.GONE
        }

        return layout
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                if (constraint.isNullOrEmpty()) {
                    filterResults.values = itemsCompleti
                    filterResults.count = itemsCompleti.size
                } else {
                    val query = constraint.toString().lowercase(Locale.getDefault())
                    val match = itemsCompleti.filter {
                        it.codice.lowercase(Locale.getDefault()).contains(query) ||
                                it.descrizione.lowercase(Locale.getDefault()).contains(query)
                    }
                    filterResults.values = match
                    filterResults.count = match.size
                }
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null && results.count > 0) {
                    itemsFiltrati = results.values as List<Articolo>
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }
}