package com.factorymarket.rxelm.ds

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class DoubleLinkedListTest {

    @Test
    fun testAddLast() {
        val list = DoubleLinkedList<Int>()
        for (i in 0..10) {
            list.addLast(i)
        }
        list.forEachIndexed { index, i ->
            assertEquals(i, index)
        }
    }

    @Test
    fun testAddFirst() {
        val list = DoubleLinkedList<Int>()
        for (i in 10 downTo 0) {
            list.addFirst(i)
        }
        list.forEachIndexed { index, i ->
            assertEquals(i, index)
        }
    }

    @Test
    fun testRemoveFirst() {
        val list = DoubleLinkedList<Int>()
        for (i in 10 downTo 0) {
            list.addFirst(i)
        }

        for (i in 10 downTo 0) {
            list.removeFirst()
        }

        assertEquals(list.isEmpty(), true)
    }

    @Test
    fun testRemoveLast() {
        val list = DoubleLinkedList<Int>()
        for (i in 10 downTo 0) {
            list.addFirst(i)
        }

        for (i in 10 downTo 0) {
            list.removeLast()
        }

        assertEquals(list.isEmpty(), true)
    }

    @Test
    fun tesBackAndForth() {
        val list = DoubleLinkedList<Int>()
        list.addFirst(1)
        list.addFirst(2)
        list.addFirst(3)
        list.addLast(9)
        list.addLast(8)
        list.addLast(7)
        assertEquals(list.first(), 3)
        list.removeFirst()
        assertEquals(list.first(), 2)
        list.removeFirst()
        assertEquals(list.first(), 1)
        list.removeFirst()
        assertEquals(list.first(), 9)
        list.removeFirst()
        assertEquals(list.first(), 8)
        list.removeFirst()
        assertEquals(list.first(), 7)
        list.removeFirst()

        assertEquals(list.isEmpty(), true)
    }

    @Test
    fun testBackAndForth2() {
        val list = DoubleLinkedList<Int>()
        list.addFirst(1)
        list.addFirst(2)
        list.addFirst(3)
        list.addLast(9)
        list.addLast(8)
        list.addLast(7)
        assertEquals(list.last(), 7)
        list.removeLast()
        assertEquals(list.last(), 8)
        list.removeLast()
        assertEquals(list.last(), 9)
        list.removeLast()
        assertEquals(list.last(), 1)
        list.removeLast()
        assertEquals(list.last(), 2)
        list.removeLast()
        assertEquals(list.last(), 3)
        list.removeLast()

        assertEquals(list.isEmpty(), true)
    }

    @Test
    fun iterator() {
        val list = DoubleLinkedList<Int>()
        list.addFirst(3)
        list.addFirst(2)
        list.addFirst(1)
        list.addLast(4)
        list.addLast(5)
        list.addLast(6)
        val iter = list.iterator()
        var i = 1
        while (iter.hasNext()) {
            val value = iter.next()
            assertEquals(value, i)
            i++
        }
    }

    @Test
    fun clear(){
        val list = DoubleLinkedList<Int>()
        list.addFirst(3)
        list.addFirst(2)
        list.addFirst(1)
        list.addLast(4)
        list.addLast(5)
        list.addLast(6)
        list.clear()
        assertEquals(list.isEmpty(), true)
    }
}