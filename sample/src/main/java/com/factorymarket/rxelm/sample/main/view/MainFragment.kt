package com.factorymarket.rxelm.sample.main.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import com.factorymarket.rxelm.sample.BaseFragment
import com.factorymarket.rxelm.sample.R
import com.factorymarket.rxelm.sample.main.di.MainModule
import com.factorymarket.rxelm.sample.main.presenter.MainPresenter
import org.eclipse.egit.github.core.Repository
import javax.inject.Inject

class MainFragment : BaseFragment(), IMainView {

    @Inject lateinit var presenter: MainPresenter
    @BindView(R.id.repos_list) lateinit var reposList: RecyclerView
    @BindView(R.id.repos_progress) lateinit var progressBar: ProgressBar
    @BindView(R.id.error_text) lateinit var errorText: TextView
    @BindView(R.id.send) lateinit var sendBtn: Button
    @BindView(R.id.cancel) lateinit var cancelBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getActivityComponent()
            .plusMainComponent(MainModule(this))
            .inject(this)
    }


    override fun getLayoutRes(): Int = R.layout.main_layout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reposList.layoutManager = LinearLayoutManager(activity)

        sendBtn.setOnClickListener {
            presenter.send()
        }

        cancelBtn.setOnClickListener {
            presenter.cancel()
        }

        presenter.init(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.destroy()
    }

    override fun setTitle(title: String) {
        (activity as AppCompatActivity).supportActionBar?.title = title
    }

    override fun showProgress() {
        progressBar.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        progressBar.visibility = View.GONE
    }

    override fun setErrorText(errorText: String) {
        this.errorText.text = errorText
    }

    override fun showErrorText() {
        errorText.visibility = View.VISIBLE
    }

    override fun setRepos(reposList: List<Repository>) {
        this.reposList.adapter = ReposAdapter(reposList, layoutInflater)
    }

    private inner class ReposAdapter(private val repos: List<Repository>, private val inflater: LayoutInflater) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return RepoViewHolder(inflater.inflate(R.layout.repos_list_item_layout, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as RepoViewHolder).bind(repos[position])
        }

        override fun getItemCount(): Int {
            return repos.size
        }

        internal inner class RepoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            var repoName: TextView = itemView.findViewById(R.id.repo_name) as TextView
            var repoStarsCount: TextView = itemView.findViewById(R.id.repo_stars_count) as TextView

            fun bind(repository: Repository) {
                repoName.text = repository.name
                repoStarsCount.text = "watchers:" + repository.watchers
            }
        }
    }


}