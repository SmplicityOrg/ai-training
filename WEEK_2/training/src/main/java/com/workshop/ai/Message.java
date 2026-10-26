package com.workshop.ai;

public class Message {
	
	private String author;
	
	private String content;

	public Message(String author, String content) {
		super();
		this.author = author;
		this.content = content;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
	
	
	public String toString() {
		return this.author + " :" + this.content + "\n";
	}

}
