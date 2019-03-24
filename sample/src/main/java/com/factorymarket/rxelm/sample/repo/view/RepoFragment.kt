package com.factorymarket.rxelm.sample.repo.view

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import butterknife.BindView
import com.factorymarket.rxelm.sample.BaseFragment
import com.factorymarket.rxelm.sample.R
import com.factorymarket.rxelm.sample.repo.di.RepoModule
import com.factorymarket.rxelm.sample.repo.presenter.RepoPresenter
import org.eclipse.egit.github.core.Repository
import javax.inject.Inject

class RepoFragment : BaseFragment(), IRepoView {

    companion object {
        const val REPO_ID_KEY = "repo_id_key"
    }

    @Inject lateinit var presenter: RepoPresenter

    @BindView(R.id.tvRepoName) lateinit var tvRepoName: TextView
    @BindView(R.id.tvRepoDescr) lateinit var tvRepoDescr: TextView
    @BindView(R.id.tvOwner) lateinit var tvRepoOwner: TextView
    @BindView(R.id.pbLoading) lateinit var pbLoading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getActivityComponent()
                .plusRepoComponent(RepoModule(this, arguments?.getString(REPO_ID_KEY) ?: ""))
                .inject(this)
    }

    override fun getLayoutRes(): Int = R.layout.repo_layout

    override fun onResume() {
        super.onResume()
        presenter.init()
    }

    override fun showLoading(loading: Boolean) {
        tvRepoName.visibility = View.GONE
        tvRepoDescr.visibility = View.GONE
        tvRepoOwner.visibility = View.GONE
        pbLoading.visibility = View.VISIBLE
    }

    override fun showRepo(repo: Repository) {
        tvRepoName.visibility = View.VISIBLE
        tvRepoDescr.visibility = View.VISIBLE
        tvRepoOwner.visibility = View.VISIBLE
        pbLoading.visibility = View.GONE

        tvRepoName.text = repo.name
        tvRepoDescr.text = repo.description
        tvRepoOwner.text = repo.owner.login
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.destroy()
    }

}