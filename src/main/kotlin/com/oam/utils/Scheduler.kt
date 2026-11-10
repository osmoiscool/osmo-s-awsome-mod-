package com.oam.utils

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object Scheduler {

    private class Task(var ticks: Int, val action: () -> Unit)

    private val tickTasks = ConcurrentLinkedQueue<Task>()
    private val timer = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "OAM-Scheduler").apply { isDaemon = true }
    }

    fun afterTicks(ticks: Int, action: () -> Unit) {
        tickTasks.add(Task(ticks, action))
    }

    fun afterMs(ms: Long, action: () -> Unit) {
        if (ms <= 0) {
            mc.execute(action)
            return
        }
        timer.schedule({ mc.execute(action) }, ms, TimeUnit.MILLISECONDS)
    }

    fun onTickStart() {
        val it = tickTasks.iterator()
        while (it.hasNext()) {
            val task = it.next()
            if (task.ticks-- <= 0) {
                it.remove()
                task.action()
            }
        }
    }
}
