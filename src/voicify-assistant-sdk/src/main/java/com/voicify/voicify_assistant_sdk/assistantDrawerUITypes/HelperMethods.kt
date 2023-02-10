package com.voicify.voicify_assistant_sdk.assistantDrawerUITypes

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
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
    }
}