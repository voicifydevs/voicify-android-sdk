package com.voicify.voicify_assistant_sdk.components.body

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.voicify.voicify_assistant_sdk.R
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.BodyProps

internal class HintsRecyclerViewAdapter(private var hintsList: List<String>, private var bodyProps: BodyProps?, private var configurationBodyProps: BodyProps?, private var onHintClicked: ((hint: String) -> Unit), private var context: Context) :
    RecyclerView.Adapter<HintsRecyclerViewAdapter.MyViewHolder>() {
    internal inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var hintsTextView: TextView = view.findViewById(R.id.hintTextView)
        var hintsContainerLayout: LinearLayout = view.findViewById(R.id.hintsContainerLinearLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.hints_recycleview_row, parent, false)
        return MyViewHolder(itemView)
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val hint = hintsList[position]
        val hintsContainer = holder.hintsContainerLayout
        val hintTextView = holder.hintsTextView
        hintTextView.minWidth = 100

        val hintsContainerLayoutStyle = GradientDrawable()
        hintsContainerLayoutStyle.cornerRadius = bodyProps?.hintsBorderRadius ?: configurationBodyProps?.hintsBorderRadius ?: 60f
        hintsContainerLayoutStyle.setStroke(
            bodyProps?.hintsBorderWidth ?: configurationBodyProps?.hintsBorderWidth ?: 4,
            Color.parseColor(bodyProps?.hintsBorderColor ?: configurationBodyProps?.hintsBorderColor ?: context.getString(R.string.gray))
        )
        hintsContainerLayoutStyle.setColor(Color.parseColor(bodyProps?.hintsBackgroundColor ?: configurationBodyProps?.hintsBackgroundColor ?: context.getString(R.string.white)))
        hintsContainer.background = hintsContainerLayoutStyle

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(16, 0 , 0, 0)
        hintsContainer.layoutParams = layoutParams
        hintsContainer.setPadding(
            bodyProps?.hintsPaddingLeft ?: configurationBodyProps?.hintsPaddingLeft ?: 40,
            bodyProps?.hintsPaddingTop ?: configurationBodyProps?.hintsPaddingTop ?: 20,
            bodyProps?.hintsPaddingRight ?: configurationBodyProps?.hintsPaddingRight ?: 40,
            bodyProps?.hintsPaddingBottom ?: configurationBodyProps?.hintsPaddingBottom ?: 20,
        )

        hintTextView.textSize = bodyProps?.hintsFontSize ?: configurationBodyProps?.hintsFontSize ?: 14f
        hintTextView.typeface = Typeface.create(
            bodyProps?.hintsFontFamily ?: configurationBodyProps?.hintsFontFamily ?: context.getString(R.string.default_font),
            Typeface.NORMAL
        )
        hintTextView.setTextColor(Color.parseColor(bodyProps?.hintsTextColor ?: configurationBodyProps?.hintsTextColor ?: context.getString(R.string.gray)))
        hintTextView.text = hint
        hintsContainer.setOnClickListener{
            onHintClicked(hint)
        }
    }
    override fun getItemCount(): Int {
        return hintsList.size
    }

}