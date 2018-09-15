package com.factorymarket.rxelm.sample.login.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.factorymarket.rxelm.program.ProgramBuilder
import com.factorymarket.rxelm.sample.navigation.AndroidNavigator
import com.factorymarket.rxelm.sample.R
import com.factorymarket.rxelm.sample.SampleApp
import com.factorymarket.rxelm.sample.data.AppPrefs
import com.factorymarket.rxelm.sample.login.presenter.LoginPresenter
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class LoginFragment : Fragment(), ILoginView {

    private var viewDisposables: CompositeDisposable = CompositeDisposable()

    lateinit var presenter: LoginPresenter
    lateinit var loginInput: TextInputLayout
    lateinit var loginText: TextInputEditText
    lateinit var passwordInput: TextInputLayout
    lateinit var passwordText: TextInputEditText
    lateinit var loginBtn: Button
    lateinit var errorTxt: TextView
    lateinit var loginProgress: ProgressBar
    lateinit var saveCredentialsCb: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = context ?: return

        presenter = LoginPresenter(
            this,
            ProgramBuilder()
                .outputScheduler(AndroidSchedulers.mainThread()),
            AppPrefs(ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)),
            (activity?.application as SampleApp).service,
            AndroidNavigator(activity!!)
        )
        presenter.init()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        loginInput = view.findViewById(R.id.login_til) as TextInputLayout
        loginText = view.findViewById(R.id.login) as TextInputEditText
        passwordInput = view.findViewById(R.id.password_til) as TextInputLayout
        passwordText = view.findViewById(R.id.password) as TextInputEditText
        loginBtn = view.findViewById(R.id.login_btn) as Button
        errorTxt = view.findViewById(R.id.error) as TextView
        loginProgress = view.findViewById(R.id.login_progress) as ProgressBar
        saveCredentialsCb = view.findViewById(R.id.save_credentials_cb) as CheckBox
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewDisposables.add(presenter.addLoginInput(RxTextView.textChanges(loginText)))
        viewDisposables.add(presenter.addPasswordInput(RxTextView.textChanges(passwordText)))
        loginBtn.setOnClickListener { presenter.loginBtnClick() }
        saveCredentialsCb.setOnCheckedChangeListener { buttonView, isChecked ->
            hideKeyboard()
            presenter.onSaveCredentialsCheck(isChecked)
        }

        presenter.render()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!viewDisposables.isDisposed) {
            viewDisposables.dispose()
            viewDisposables = CompositeDisposable()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.destroy()
    }

    override fun setProgress(show: Boolean) {
        loginProgress.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun showPasswordError(errorText: String?) {
        errorText?.let {
            passwordInput.error = errorText
        } ?: run {
            passwordInput.error = ""
        }
    }

    override fun showLoginError(errorText: String?) {
        errorText?.let {
            loginInput.error = errorText
        } ?: run {
            loginInput.error = ""
        }
    }

    override fun setError(error: String?) {
        error?.let {
            errorTxt.visibility = View.VISIBLE
            errorTxt.text = error
        } ?: run {
            errorTxt.visibility = View.GONE
        }
    }

    override fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?

        imm?.hideSoftInputFromWindow(loginText.windowToken, 0)
    }

    override fun setEnableLoginBtn(enabled: Boolean) {
        loginBtn.isEnabled = enabled
    }


}
