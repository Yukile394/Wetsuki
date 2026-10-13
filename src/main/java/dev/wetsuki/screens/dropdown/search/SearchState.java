package dev.wetsuki.screens.dropdown.search;


public class SearchState {
    public String text = "";
    public boolean focused = false;
    public int cursorPosition = 0;
    public long lastCursorBlink = System.currentTimeMillis();
    public boolean cursorVisible = true;


    public SearchState() {
    }
}
