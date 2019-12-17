package dev.teapot.sample.navigation

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import dev.teapot.sample.MAIN_TAG
import dev.teapot.sample.R
import dev.teapot.sample.REPO_TAG
import dev.teapot.sample.main.view.MainFragment
import dev.teapot.sample.repo.view.RepoFragment
import dev.teapot.sample.repo.view.RepoFragment.Companion.REPO_ID_KEY
import org.eclipse.egit.github.core.Repository


class AndroidNavigator(private val activity: FragmentActivity) : Navigator {

    override fun goToMainScreen() {
        activity
            .supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment, MainFragment(), MAIN_TAG)
            .commitNow()
    }

    override fun goToRepo(repository: Repository) {
        val fragment = RepoFragment()
        val bundle = Bundle()
        bundle.putString(REPO_ID_KEY, repository.htmlUrl)
        fragment.arguments = bundle
        activity
            .supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment, fragment, REPO_TAG)
            .addToBackStack(null)
            .commit()
    }

}