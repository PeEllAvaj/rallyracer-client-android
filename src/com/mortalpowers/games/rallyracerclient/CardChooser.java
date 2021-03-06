package com.mortalpowers.games.rallyracerclient;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class CardChooser extends View {
	private ArrayList<Card> cards;
	Card inMotion = null;
	RallyRacerClientGame context;
	/**
	 * Can only invalidate from a thread that created this view.
	 * 
	 * @param m
	 */
	public void addCard(Card m) {
		cards.add(m);
		Log.d("cardchooser", "Added a card with contents " + m.msg
				+ " to the hand.");
		cleanUp();
	}

	public void clearCards() {
		cards.clear();
	}

	public CardChooser(RallyRacerClientGame context) {
		super(context);
		this.context = context;
		setFocusable(true); // necessary for getting the touch events

		// TODO Auto-generated constructor stub
		cards = new ArrayList<Card>();
	}

	protected void onDraw(Canvas canvas) {
		// canvas.drawColor(0xFFCCCCCC); //if you want another background color
		/**
		 * Regular for-loop used here to avoid concurrent modifications with
		 * network thread.
		 */
		for (int i = 0; i < cards.size(); i++) {
			Card c = cards.get(i);
			canvas.drawText(c.msg, c.x, c.y, c.p);
		}
		Paint white = new Paint();
		white.setARGB(255, 200, 0, 0);
		canvas.drawRect(30, 375, 300, 400, white);
	}

	public void cleanUp() {
		int i = 50;
		for (Card m : cards) {
			m.y = i;
			i += 75;
		}
		invalidate();
	}

	public boolean onTouchEvent(MotionEvent event) {
		int eventaction = event.getAction();

		int X = (int) event.getX();
		int Y = (int) event.getY();

		switch (eventaction) {

		case MotionEvent.ACTION_DOWN: // touch down so check if the finger is on
			invalidate();
			if (Y < 375) {
				for (Card m : cards) {
					// check if inside the bounds of the ball (circle)
					// get the center for the ball
					if (m == null) {
						Log.e("cardevent", "Why was the card m null?!");
					}
					float centerY = m.y - 5;

					if (Math.abs(centerY - Y) < 20) {
						invalidate();
						inMotion = m;
						break;
					}

				}
			} else {
				/**
				 * No cards yet, send start game.
				 */
				if (cards.size() == 0 || cards.size() == 1) {
					String n = Network
							.request("game-server.php?action=startGame");

					try {
						JSONArray cards = new JSONArray(new JSONTokener(n));
						clearCards();
						for (int i = 0; i < cards.length(); i++) {
							JSONObject card = cards.getJSONObject(i);
							String msg = card.getString("priority") + ","
									+ card.getString("action") + ","
									+ card.getString("quantity");
							addCard(new CardChooser.Card(msg));
						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						context.debugMsg("Invalid JSON received from server.");
					}
				} else if (cards.size() == 5) {
					String order = "";
					for (Card m : cards) {
						order += m.msg + ";";
					}
					Network
							.request("game-server.php?action=sendCommand&command="
									+ order);
					clearCards();
				}
			}

			break;

		case MotionEvent.ACTION_MOVE: // touch message with the finger
			if (inMotion != null) {
				inMotion.y = Y + 15;
				invalidate();
			}

			break;

		case MotionEvent.ACTION_UP:
			Log.e("cardevent", "Up at " + Y);
			if (inMotion != null) {
				cards.remove(inMotion);
				int i = 0;
				for (Card m : cards) {

					if (inMotion.y < m.y) {
						break;
					}
					i++;

				}

				cards.add(i, inMotion);
				inMotion = null;

			}
			cleanUp();
			break;
		}
		return true;

	}

	static public class Card {
		String msg;
		float x, y;
		Paint p;

		public Card(String s) {
			msg = s;
			p = new Paint();
			p.setStrokeWidth(2);
			p.setARGB(255, 200, 200, 200);
			p.setTextSize(42);
		}
	}
}
