package com.factorymarket.rxelm.sample.main.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.factorymarket.rxelm.log.RxElmLogger
import com.factorymarket.rxelm.program.ProgramBuilder
import com.factorymarket.rxelm.sample.R
import com.factorymarket.rxelm.sample.SampleApp
import com.factorymarket.rxelm.sample.main.presenter.MainPresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import org.eclipse.egit.github.core.Repository
import timber.log.Timber

class MainFragment : Fragment(), IMainView {

    private lateinit var presenter: MainPresenter
    private lateinit var reposList: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = MainPresenter(
            this,
            ProgramBuilder()
                .outputScheduler(AndroidSchedulers.mainThread())
                .logger(object : RxElmLogger {
                    override fun log(stateName: String, message: String) {
                        Timber.tag(stateName).d(message)
                    }
                }),
            (activity?.application as SampleApp).service
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.main_layout, container, false)
        reposList = view.findViewById(R.id.repos_list)
        reposList.layoutManager = LinearLayoutManager(activity)
        progressBar = view.findViewById(R.id.repos_progress)
        errorText = view.findViewById(R.id.error_text)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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