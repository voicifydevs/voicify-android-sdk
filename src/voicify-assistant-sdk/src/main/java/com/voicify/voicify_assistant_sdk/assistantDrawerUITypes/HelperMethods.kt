package com.voicify.voicify_assistant_sdk.assistantDrawerUITypes

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.SpanFactory
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.ext.tasklist.TaskListItem
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.ext.tasklist.TaskListSpan
import java.lang.Exception

class HelperMethods {
    companion object {
        fun getPixelsFromDp(dp: Int, scale: Float): Int {
            return (dp * scale + 0.5f).toInt()
        }

        fun loadImageFromUrl(url: String, view: ImageView, imageColor: String? = null,  resources: Resources? = null, isRounded: Boolean = false, borderRadius: Float = 0f){
            if(isRounded && resources != null)
            {
                Picasso.get().load(url).into(view , object: Callback {
                    override fun onSuccess() {
                        val imageBitmap = view.drawable as BitmapDrawable
                        val bitmap = imageBitmap.bitmap
                        val imageDrawable = RoundedBitmapDrawableFactory.create(resources, bitmap);
                        imageDrawable.isCircular = true;
                        imageDrawable.cornerRadius = borderRadius
                        view.setImageDrawable(imageDrawable);
                        if(!imageColor.isNullOrEmpty())
                        {
                            DrawableCompat.setTint(view.drawable, Color.parseColor(imageColor))
                        }
                    }
                    override fun onError(e: Exception?) {
                    }
                });
            }
            else if(imageColor.isNullOrEmpty())
            {
                Picasso.get().load(url).into(view)
            }
            else{
                Picasso.get().load(url).into(view, object: Callback {
                    override fun onSuccess() {
                        DrawableCompat.setTint(view.drawable, Color.parseColor(imageColor))
                    }

                    override fun onError(e: Exception?) {
                    }
                })
            }
        }
        fun buildMarkwon(context: Context, openLink: (String) -> Unit): Markwon{
            return Markwon.builder(context)
                .usePlugin(CorePlugin.create())
                .usePlugin(TaskListPlugin.create(context))
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {

                        // obtain original SpanFactory set by TaskListPlugin
                        val origin = builder.getFactory(TaskListItem::class.java)
                            ?: // or throw, as it's a bit weird state and we expect
                            // this factory to be present
                            return
                        builder.setFactory(
                            TaskListItem::class.java,
                            SpanFactory { configuration, props ->
                                // it's a bit non-secure behavior and we should validate
                                // the type of returned span first, but for the sake of brevity
                                // we skip this step
                                val span = origin.getSpans(configuration, props) as TaskListSpan?
                                    ?: // or throw
                                    return@SpanFactory null

                                // return an array of spans
                                arrayOf(
                                    span,
                                    object : ClickableSpan() {
                                        override fun onClick(widget: View) {
                                            // toggle VISUAL state
                                            span.isDone = !span.isDone
                                            // do not forget to invalidate widget
                                            widget.invalidate()

                                            // execute your persistence logic
                                        }

                                        override fun updateDrawState(ds: TextPaint) {
                                            // no-op, so appearance is not changed (otherwise
                                            // task list item will look like a link)
                                        }
                                    }
                                )
                            })
                    }
                })
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                        super.configureConfiguration(builder)
                        builder.linkResolver { view, link ->
                            openLink(link)
                        }
                    }
                }).build()
        }
    }
}