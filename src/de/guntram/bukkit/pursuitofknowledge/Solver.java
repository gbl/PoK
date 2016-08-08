/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.bukkit.pursuitofknowledge;

import org.bukkit.Bukkit;

/**
 *
 * @author gbl
 */
public class Solver implements Runnable {

    private final PoK plugin;
    
    Solver(PoK plugin) {
        this.plugin=plugin;
    }

    @Override
    public void run() {
        QA qa=plugin.getQAList().currentQA();
        // Bukkit.broadcast(qa.answer, "poc.answer");
        plugin.solve(qa.getShowableAnswer());
        if (!plugin.getQAList().hasMoreQuestions()) {
            // TODO: use endmessage somehow, and give prizes. SHould be
            // a method in plugin that gets called from here.
            plugin.nextGameMode();
        } else {
            plugin.scheduleNextAsker();
        }
    }
}