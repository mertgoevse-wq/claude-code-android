package dev.ccandroid.core

public interface ClipboardGateway {
    public fun setText(text: String)
    public fun getText(): String?
}
