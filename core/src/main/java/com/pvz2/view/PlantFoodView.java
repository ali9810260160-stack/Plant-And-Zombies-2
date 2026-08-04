package com.pvz2.view;

/**
 * کلاس View مخصوص پیام‌های اثرات Plant Food در منوی بازی.
 * تمام System.out های مربوط به Plant Food از PlantFoodEffectHandler
 * به این کلاس منتقل شده‌اند.
 */
public class PlantFoodView {

    public void printSunProduced(int amount) {
        System.out.println("  🌞 Plant food: +" + amount + " sun!");
    }

    public void printRapidFire(int count, String targetDesc) {
        System.out.println("  🔥 Plant food: rapid fire " + count
                + " shots" + (targetDesc.isEmpty() ? "!" : " at " + targetDesc + "!"));
    }

    public void printFiredAllLanes(int rows) {
        System.out.println("  🔥 Plant food: fired at all " + rows + " lanes!");
    }

    public void printOmnidirectionalFire() {
        System.out.println("  🔥 Plant food: omnidirectional rapid fire!");
    }

    public void printPiercingShots(int count) {
        System.out.println("  🎯 Plant food: " + count + " piercing shots!");
    }

    public void printHomingShots(int count) {
        System.out.println("  🎯 Plant food: " + count + " homing shots!");
    }

    public void printLaneCleared(int row, int killed) {
        System.out.println("  💥 Plant food: cleared lane " + row
                + " (" + killed + " zombies)!");
    }

    public void printFrozeLane(int row) {
        System.out.println("  ❄️ Plant food: froze all zombies in lane " + row + "!");
    }

    public void printFrozeAll() {
        System.out.println("  ❄️ Plant food: ALL zombies frozen!");
    }

    public void printHypnotized(int n) {
        System.out.println("  🌀 Plant food: hypnotized " + n + " zombie(s)!");
    }

    public void printInstantKill(int n) {
        System.out.println("  ⚡ Plant food: killed " + n + " zombie(s) instantly!");
    }

    public void printChomperSwallowed(int count) {
        System.out.println("  😋 Plant food: Chomper swallowed " + count + " zombies!");
    }

    public void printAoeBlast(int size, int killed) {
        System.out.println("  💥 Plant food: AoE " + size + "x" + size
                + " blast — killed " + killed + " zombies!");
    }

    public void printPushBack(int row) {
        System.out.println("  💨 Plant food: pushed back all zombies in lane " + row + "!");
    }

    public void printButtered(int row) {
        System.out.println("  🧈 Plant food: buttered all zombies in lane " + row + "!");
    }

    public void printLobbedBalls(int count) {
        System.out.println("  🏐 Plant food: lobbed " + count + " heavy balls!");
    }

    public void printRemovedArmors(int removed) {
        System.out.println("  🔮 Plant food: removed " + removed + " armor(s)!");
    }

    public void printStunnedNearby() {
        System.out.println("  ✨ Plant food: stunned nearby zombies!");
    }
}
