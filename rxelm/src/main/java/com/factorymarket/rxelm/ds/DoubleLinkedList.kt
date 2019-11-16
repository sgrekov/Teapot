package com.factorymarket.rxelm.ds


internal class DoubleLinkedList<E>() : MutableIterable<E> {

    private class Node<E>(var prev: Node<E>?, var item: E, var next: Node<E>?)

    var size = 0
        get

    private var first: Node<E>? = null

    private var last: Node<E>? = null

    fun addLast(value: E) {
        when (size) {
            0 -> {
                addInitial(value)
            }
            else -> {
                val newValue = Node(last, value, null)
                last?.next = newValue
                last = newValue
            }
        }
        size++
    }

    fun addFirst(value: E) {
        when (size) {
            0 -> {
                addInitial(value)
            }
            else -> {
                val newValue = Node(null, value, first)
                first?.prev = newValue
                first = newValue
            }
        }
        size++
    }

    fun isEmpty(): Boolean = size == 0

    fun first(): E {
        if (isEmpty())
            throw NoSuchElementException("List is empty.")
        return first!!.item
    }

    fun last(): E {
        if (isEmpty())
            throw NoSuchElementException("List is empty.")
        return last!!.item
    }


    fun removeFirst() {
        if (isEmpty())
            throw NoSuchElementException("List is empty.")
        val nodeToDelete = first
        first = nodeToDelete?.next
        first?.prev = null
        nodeToDelete?.next = null
        size--
    }

    fun removeLast() {
        if (isEmpty())
            throw NoSuchElementException("List is empty.")
        val nodeToDelete = last
        last = nodeToDelete?.prev
        nodeToDelete?.prev = null
        last?.next = null
        size--
    }

    private fun addInitial(value: E) {
        val newValue = Node(null, value, null)
        last = newValue
        first = newValue
    }

    override fun iterator(): MutableIterator<E> {
        return object : MutableIterator<E> {
            var current: Node<E>? = first

            override fun hasNext(): Boolean = current != null && current?.next != null

            override fun next(): E {
                val value = current?.item
                current = current?.next
                return value!!
            }

            override fun remove() {
                val nodeToDelete = current?.prev
                val nodeBefore = nodeToDelete?.prev
                nodeBefore?.next = current
                current?.prev = nodeBefore
                nodeToDelete?.next = null
                nodeToDelete?.prev = null
                size--
            }

        }
    }

    fun clear() {
        while (first != null) {
            val temp = first
            first = first?.next
            temp?.next = null
            temp?.prev = null
            size--
        }
        last?.prev = null
        last = null
    }

}