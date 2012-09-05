package com.iconnex.gwthangman.client;

import com.iconnex.gwthangman.shared.FieldVerifier;
//import com.google.appengine.api.urlfetch.HTTPRequest;
//import org.apache.camel.component.gae.http.GHttpBinding;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
//import com.google.gwt.user.client.*;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ResponseTextHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class GwtHangman implements EntryPoint {
	/**
	 * The message displayed to the user when the server cannot be reached or
	 * returns an error.
	 */
	private static final String SERVER_ERROR = "An error occurred while "
			+ "attempting to contact the server. Please check your network "
			+ "connection and try again.";

	/**
	 * Create a remote service proxy to talk to the server-side Greeting service.
	 */
	private final GreetingServiceAsync greetingService = GWT
			.create(GreetingService.class);

	private FlowPanel letters = new FlowPanel();
	private Label wordLabel = new Label();
	private Image image = new Image();
	
	
	private final int MAX_GUESSES = 6;
	private int misses;
	private String[] words;
	private String word;
	private char[] visibleWord;
	
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		final Button sendButton = new Button("Send");
		final TextBox nameField = new TextBox();
		nameField.setText("GWT User");
		final Label errorLabel = new Label();
		

		
		// We can add style names to widgets
		sendButton.addStyleName("sendButton");

		// Add the nameField and sendButton to the RootPanel
		// Use RootPanel.get() to get the entire body element
		RootPanel rootPanel = RootPanel.get("nameFieldContainer");
		rootPanel.add(nameField, 10, 460);
		RootPanel.get("sendButtonContainer").add(sendButton, 10, 500);
		RootPanel.get("errorLabelContainer").add(errorLabel);

		// Focus the cursor on the name field when the app loads
		nameField.setFocus(true);
		
		letters = new FlowPanel();
		rootPanel.add(letters, 10, 10);
		letters.setSize("419px", "26px");
		
		image = new Image();
		image.setUrl("gwthangman/hm1.gif");
		rootPanel.add(image, 41, 72);
		image.setSize("324px", "304px");
		
		wordLabel = new Label();
		rootPanel.add(wordLabel, 6, 382);
		wordLabel.setSize("434px", "27px");
		nameField.selectAll();

		//load words
		RequestBuilder requestBuilder = new RequestBuilder( RequestBuilder.GET, "gwthangman/movies.txt" );
		try {
			requestBuilder.sendRequest( null, new RequestCallback(){

				public void onError(Request request, Throwable exception) {
					GWT.log( "failed getting movie list", exception );
				}

				public void onResponseReceived(Request request, Response response) {
					words = response.getText().split("\n");
					//임시
/*					String A = "time after time" ;					
					words = A.split("\n");*/
					
					startGame();
				}} );
		} catch (RequestException e) {
			GWT.log( "failed getting movie list", e );
		}
		

		
/*		HTTPRequest.asyncGet("gwthangman/gwt/public/public/movies.txt", new ResponseTextHandler(){
			public void onCompletion(String responseText) {
				words = responseText.split("\n");
				startGame();
			}
		});*/
		
		// Create the popup dialog box
		final DialogBox dialogBox = new DialogBox();
		dialogBox.setText("Remote Procedure Call");
		dialogBox.setAnimationEnabled(true);
		final Button closeButton = new Button("Close");
		// We can set the id of a widget by accessing its Element
		closeButton.getElement().setId("closeButton");
		final Label textToServerLabel = new Label();
		final HTML serverResponseLabel = new HTML();
		VerticalPanel dialogVPanel = new VerticalPanel();
		dialogVPanel.addStyleName("dialogVPanel");
		dialogVPanel.add(new HTML("<b>Sending name to the server:</b>"));
		dialogVPanel.add(textToServerLabel);
		dialogVPanel.add(new HTML("<br><b>Server replies:</b>"));
		dialogVPanel.add(serverResponseLabel);
		dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
		dialogVPanel.add(closeButton);
		dialogBox.setWidget(dialogVPanel);

		// Add a handler to close the DialogBox
		closeButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
				sendButton.setEnabled(true);
				sendButton.setFocus(true);
			}
		});


				
		
		// Create a handler for the sendButton and nameField
		class MyHandler implements ClickHandler, KeyUpHandler {
			/**
			 * Fired when the user clicks on the sendButton.
			 */
			public void onClick(ClickEvent event) {
				sendNameToServer();
			}

			/**
			 * Fired when the user types in the nameField.
			 */
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					sendNameToServer();
				}
			}

			/**
			 * Send the name from the nameField to the server and wait for a response.
			 */
			private void sendNameToServer() {
				// First, we validate the input.
				errorLabel.setText("");
				String textToServer = nameField.getText();
				if (!FieldVerifier.isValidName(textToServer)) {
					errorLabel.setText("Please enter at least four characters");
					return;
				}

				// Then, we send the input to the server.
				sendButton.setEnabled(false);
				textToServerLabel.setText(textToServer);
				serverResponseLabel.setText("");
				greetingService.greetServer(textToServer,
						new AsyncCallback<String>() {
							public void onFailure(Throwable caught) {
								// Show the RPC error message to the user
								dialogBox
										.setText("Remote Procedure Call - Failure");
								serverResponseLabel
										.addStyleName("serverResponseLabelError");
								serverResponseLabel.setHTML(SERVER_ERROR);
								dialogBox.center();
								closeButton.setFocus(true);
							}

							public void onSuccess(String result) {
								dialogBox.setText("Remote Procedure Call");
								serverResponseLabel
										.removeStyleName("serverResponseLabelError");
								serverResponseLabel.setHTML(result);
								dialogBox.center();
								closeButton.setFocus(true);
							}
						});
			}
		}

		// Add a handler to send the name to the server
		MyHandler handler = new MyHandler();
		sendButton.addClickHandler(handler);
		nameField.addKeyUpHandler(handler);
		
		//String A = "time after time" ;
		
	//	words = A.split("\n");
		//startGame();
		
		
		
	}
	
	public void startGame(){
		misses = 0;
		//add letter buttons
		letters.clear();
		for( char letter = 'A'; letter <= 'Z'; letter++ ){
			final Button button = new Button(Character.toString(letter));
			
		//	closeButton.addClickHandler(new ClickHandler() {
			
			button.addClickHandler(new ClickHandler(){
				@Override
				public void onClick(ClickEvent event) {
					// TODO Auto-generated method stub
					button.setEnabled(false);
					guess( button.getText().charAt(0) );
				}
			});
			letters.add( button );
		}
		image.setUrl("gwthangman/hm1.gif");
		
		setWord(words[Random.nextInt(words.length)]);
	}
	
	
	public void setWord( String newWord ){
		word = newWord.toUpperCase();
		visibleWord = new char[word.length()];
		for( int i=0; i<word.length(); i++ ){
			if( word.charAt(i) != ' ' )
				visibleWord[i]='_';
			else
				visibleWord[i]=' ';
		}
		wordLabel.setText( new String(visibleWord) );
	}
	
	public void guess( char letter ){
		boolean badGuess = true;
		boolean wordFinished = true;
		
		//check for matches for this letter
		for( int i=0; i<word.length(); i++ ){
			if( word.charAt(i)==letter ){
				visibleWord[i] = letter;
				badGuess = false;
			}
			else if( visibleWord[i]=='_'){
				wordFinished = false;
			}
		}
		wordLabel.setText( new String(visibleWord) );
		
		if( wordFinished ){
			Window.alert( "Congratulations! 축하합니다. 하하하!" );
			startGame();
		}
		else if( badGuess ){
			misses++;
			image.setUrl("gwthangman/hm"+Integer.toString(misses+1)+".gif");
			if( misses == MAX_GUESSES ){
				wordLabel.setText( word );
				Window.alert( "You ran out of guesses! The answer is "+ word);
				startGame();
			}
		}
	}
	
}
