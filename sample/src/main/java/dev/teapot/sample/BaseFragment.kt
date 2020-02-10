package dev.teapot.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import butterknife.ButterKnife
import butterknife.Unbinder
import dev.teapot.sample.main.di.ActivityComponent
import dev.teapot.sample.main.view.MainActivity

abstract class BaseFragment : Fragment() {

    lateinit var unbinder: Unbinder

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(getLayoutRes(), container, false)
        unbinder = ButterKnife.bind(this, view)
        return view
    }

    abstract fun getLayoutRes(): Int

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
    }

    fun getActivityComponent(): ActivityComponent {
        return (activity as? MainActivity)?.activityComponent!!
    }
}