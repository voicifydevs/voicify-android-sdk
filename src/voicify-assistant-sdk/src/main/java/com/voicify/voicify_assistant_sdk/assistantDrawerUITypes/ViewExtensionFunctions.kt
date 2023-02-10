package com.voicify.voicify_assistant_sdk.assistantDrawerUITypes

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import java.lang.reflect.Field

fun TextView.setCursorDrawableColor(@ColorInt color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        textCursorDrawable?.tinted(color)
        return
    }

    try {
        val editorField = TextView::class.java.getFieldByName("mEditor")
        val editor = editorField?.get(this) ?: this
        val editorClass: Class<*> = if (editorField != null) editor.javaClass else TextView::class.java
        val cursorRes = TextView::class.java.getFieldByName("mCursorDrawableRes")?.get(this) as? Int ?: return

        val tintedCursorDrawable = ContextCompat.getDrawable(context, cursorRes)?.tinted(color) ?: return

        val cursorField = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            editorClass.getFieldByName("mDrawableForCursor")
        } else {
            null
        }
        if (cursorField != null) {
            cursorField.set(editor, tintedCursorDrawable)
        } else {
            editorClass.getFieldByName("mCursorDrawableRes", "mDrawableForCursor")
                ?.set(editor, arrayOf(tintedCursorDrawable, tintedCursorDrawable))
        }
    } catch (t: Throwable) {
        t.printStackTrace()
    }
}

private fun Class<*>.getFieldByName(vararg name: String): Field? {
    name.forEach {
        try{
            return this.getDeclaredField(it).apply { isAccessible = true }
        } catch (t: Throwable) { }
    }
    return null
}

private fun Drawable.tinted(@ColorInt color: Int): Drawable = when {
    this is VectorDrawableCompat -> {
        this.apply { setTintList(ColorStateList.valueOf(color)) }
    }
    this is VectorDrawable -> {
        this.apply { setTintList(ColorStateList.valueOf(color)) }
    }
    else -> {
        DrawableCompat.wrap(this)
            .also { DrawableCompat.setTint(it, color) }
            .let { DrawableCompat.unwrap(it) }
    }
}