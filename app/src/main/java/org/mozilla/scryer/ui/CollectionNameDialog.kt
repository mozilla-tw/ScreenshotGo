package org.mozilla.scryer.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import org.mozilla.scryer.R
import org.mozilla.scryer.ScryerApplication
import org.mozilla.scryer.persistence.CollectionModel
import org.mozilla.scryer.util.ThreadUtils

class CollectionNameDialog(private val context: Context, private val delegate: Delegate) {
    var dialog: AlertDialog
    var title: String = ""

    private var editText: EditText
    private var dialogInterface: Interface

    var initialCollectionName = ""

    private val collections = mutableListOf<CollectionModel>()

    val validIcon: Drawable? by lazy {
        null
    }

    val invalidIcon: Drawable? by lazy {
        ContextCompat.getDrawable(context, R.drawable.error)?.let {
            val wrapped = DrawableCompat.wrap(it)
            DrawableCompat.setTint(wrapped, ContextCompat.getColor(context, R.color.errorRed))
            wrapped
        }
    }

    init {
        val view = View.inflate(context, R.layout.dialog_collection_name, null)
        val titleText = view.findViewById<TextView>(R.id.title)
        val editTextBar = view.findViewById<View>(R.id.edit_text_bar)

        val errorText = view.findViewById<TextView>(R.id.error_text)
        val errorIcon = view.findViewById<View>(R.id.edit_text_icon)

        editText = view.findViewById(R.id.edit_text)
        dialogInterface = object : Interface {
            override fun getInputText(): String {
                return editText.text.toString()
            }
        }

        titleText.text = context.resources.getText(R.string.collection_dialog_title_new_collection)

        dialog = AlertDialog.Builder(context, R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setPositiveButton(R.string.ac_done) { _, _ ->
                    if (dialogInterface.getInputText() == initialCollectionName) {
                        delegate.onNegativeAction(dialogInterface)
                    } else {
                        delegate.onPositiveAction(dialogInterface)
                    }
                }
                .setNegativeButton(R.string.ac_cancel) { _, _ ->
                    delegate.onNegativeAction(dialogInterface)
                }
                .setView(view)
                .create()

        val validator = InputValidator(context, object : InputValidator.ViewDelegate {
            override fun forbidContinue(forbid: Boolean) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !forbid
            }

            override fun onErrorStatusUpdate(errorMsg: String) {
                errorIcon.background = if (errorMsg.isEmpty()) { validIcon } else { invalidIcon }
                errorText.text = errorMsg

                editTextBar.setBackgroundColor(if (errorMsg.isEmpty()) {
                    ContextCompat.getColor(context, R.color.primaryTeal)
                } else {
                    ContextCompat.getColor(context, R.color.errorRed)
                })
            }

            override fun isCollectionExist(name: String): Boolean {
                return dialogInterface.getInputText() != initialCollectionName && collections.any { it.name == name }
            }
        })

        dialog.setOnShowListener {
            if (initialCollectionName.isNotEmpty()) {
                editText.setText(initialCollectionName)
                editText.setSelection(0, initialCollectionName.length)
            }
            validator.validate(dialogInterface.getInputText())
            val colors = ContextCompat.getColorStateList(context, R.color.primary_text_button)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(colors)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(colors)
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validator.validate(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    fun show() {
        if (collections.isEmpty()) {
            ThreadUtils.postToBackgroundThread {
                val list = ScryerApplication.getScreenshotRepository().getCollectionList()
                ThreadUtils.postToMainThread {
                    collections.addAll(list)
                    showImmediately()
                }
            }

        } else {
            showImmediately()
        }
    }

    private fun showImmediately() {
        dialog.show()
        editText.requestFocus()
        dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    class InputValidator(context: Context, private val viewDelegate: ViewDelegate) {
        private val lengthLimit = context.resources.getInteger(R.integer.collection_name_dialog_max_input_length)

        fun validate(input: String) {
            when {
                input.isEmpty() -> {
                    viewDelegate.forbidContinue(true)
                }

                input.length > lengthLimit -> {
                    viewDelegate.forbidContinue(true)
                    viewDelegate.onErrorStatusUpdate("At most 20 characters")
                }

                viewDelegate.isCollectionExist(input) -> {
                    viewDelegate.forbidContinue(true)
                    viewDelegate.onErrorStatusUpdate("Existed")
                }

                else -> {
                    viewDelegate.forbidContinue(false)
                    viewDelegate.onErrorStatusUpdate("")
                }
            }
        }

        interface ViewDelegate {
            fun forbidContinue(forbid: Boolean)
            fun onErrorStatusUpdate(errorMsg: String)
            fun isCollectionExist(name: String): Boolean
        }
    }

    interface Interface {
        fun getInputText(): String
    }

    interface Delegate {
        fun onPositiveAction(dialog: Interface)
        fun onNegativeAction(dialog: Interface)
    }
}
