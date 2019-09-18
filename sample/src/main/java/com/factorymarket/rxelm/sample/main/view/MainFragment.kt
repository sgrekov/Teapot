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
import com.paginate.Paginate
import org.eclipse.egit.github.core.Repository
import javax.inject.Inject

class MainFragment : BaseFragment(), IMainView {

    @Inject lateinit var presenter: MainPresenter
    @JvmField @BindView(R.id.repos_list) var reposList: RecyclerView? = null
    @JvmField @BindView(R.id.repos_progress) var progressBar: ProgressBar? = null
    @JvmField @BindView(R.id.error_text) var errorText: TextView? = null
    @JvmField @BindView(R.id.refresh) var refreshBtn: Button? = null
    @JvmField @BindView(R.id.cancel) var cancelBtn: Button? = null

    private var paginate: Paginate? = null
    lateinit var adapter : ReposAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = ReposAdapter(listOf(), layoutInflater)

        getActivityComponent()
            .plusMainComponent(MainModule(this))
            .inject(this)

        presenter.init(null)
    }


    override fun getLayoutRes(): Int = R.layout.main_layout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reposList?.layoutManager = LinearLayoutManager(activity)
        reposList?.adapter = adapter

        refreshBtn?.setOnClickListener {
            presenter.refresh()
        }

        cancelBtn?.setOnClickListener {
            presenter.cancel()
        }

        setRepos(listOf())
        setupPagination()

        presenter.render()
    }

    private fun setupPagination() {
        paginate?.unbind()

        paginate = Paginate.with(reposList, presenter)
                .setLoadingTriggerThreshold(5)
                .addLoadingListItem(false)
                .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.destroy()
    }

    override fun setTitle(title: String) {
        (activity as AppCompatActivity).supportActionBar?.title = title
    }

    override fun showProgress() {
        progressBar?.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        progressBar?.visibility = View.GONE
    }

    override fun setErrorText(errorText: String) {
        this.errorText?.text = errorText
    }

    override fun showErrorText(show : Boolean) {
        errorText?.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun setRepos(reposList: List<Repository>) {
        if (adapter.repos !== reposList){
            adapter.repos = reposList
            adapter.notifyDataSetChanged()
        }
    }

    inner class ReposAdapter(var repos: List<Repository>, private val inflater: LayoutInflater) :
        RecyclerView.Adapter<ReposAdapter.RepoViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepoViewHolder {
            return RepoViewHolder(inflater.inflate(R.layout.repos_list_item_layout, parent, false))
        }

        override fun onBindViewHolder(holder: RepoViewHolder, position: Int) {
            holder.bind(repos[position])
            holder.itemView.setOnClickListener {
                presenter.onRepoItemClick(repos[position])
            }
        }

        override fun getItemCount(): Int {
            return repos.size
        }

        inner class RepoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            var repoName: TextView = itemView.findViewById(R.id.repo_name) as TextView
            var repoStarsCount: TextView = itemView.findViewById(R.id.repo_stars_count) as TextView

            fun bind(repository: Repository) {
                repoName.text = repository.name
                repoStarsCount.text = "watchers:" + repository.watchers
            }
        }
    }


}