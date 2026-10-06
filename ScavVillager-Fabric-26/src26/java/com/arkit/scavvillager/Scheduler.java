package com.arkit.scavvillager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** 极简 tick 任务调度器：进服三连音、配方音延迟、228 弹窗延迟都用它。 */
public final class Scheduler {
    private record Task(long dueTick, Runnable action) {
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static long tick;

    public static void tick() {
        tick++;
        if (TASKS.isEmpty()) {
            return;
        }
        // 先把到期的任务摘出来再统一执行：任务执行时可能又排新任务（进服三连音就是这样），
        // 边遍历边执行会抛 ConcurrentModificationException。
        List<Task> due = null;
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            if (tick >= task.dueTick()) {
                it.remove();
                if (due == null) {
                    due = new ArrayList<>();
                }
                due.add(task);
            }
        }
        if (due == null) {
            return;
        }
        for (Task task : due) {
            try {
                task.action().run();
            } catch (Throwable t) {
                ScavVillagerClient.LOGGER.warn("[老乡村民] 延迟任务执行失败: {}", t.toString());
            }
        }
    }

    public static void after(int ticks, Runnable action) {
        TASKS.add(new Task(tick + Math.max(0, ticks), action));
    }

    public static void clear() {
        TASKS.clear();
    }

    public static long now() {
        return tick;
    }

    private Scheduler() {
    }
}
