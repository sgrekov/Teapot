package dev.teapot.sample.repo.view

import org.eclipse.egit.github.core.Repository

interface RepoView {
    fun showLoading(loading: Boolean)
    fun showRepo(repo: Repository)
}