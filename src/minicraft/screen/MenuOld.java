package minicraft.screen;

import java.awt.Dimension;
import java.awt.Point;

import minicraft.Game;
import minicraft.InputHandler;
import minicraft.Sound;
import minicraft.gfx.Rectangle;
import minicraft.gfx.Screen;
import minicraft.gfx.SpriteSheet;
import minicraft.screen.entry.ListEntry;

public class MenuOld {
	
	private static final String exitKey = "exit"; // this key control will always exit the menu
	
	// this may replace the styling fetches from the MenuData instance later
	//private FontStyle style; // the styling of the text (color, centering, etc)
	
	private Menu parent; // the menu that led to this display
	private ListEntry[] entries; // fetched from menuData
	private final boolean mutable;
	
	private Frame[] frames;
	
	private int selection;
	
	private int ticks;
	
	// menus should not be instantiated directly; instead, an instance of MenuData will instantiate it
	private Menu(Frame frame, ListEntry... entries) {
		this(false, frame, entries);
	}
	private Menu(boolean entriesAreMutable, Frame frame, ListEntry... entries) {
		this.mutable = entriesAreMutable;
		if(data instanceof TitleMenu)
			this.parent = this;
		else
			this.parent = Game.getMenu();
		entries = data.getEntries();
		if(entries.length == 0)
			selection = -1;
		else
			selection = 0;
		
		if(frames == null)
			this.frames = new Frame[0];
		else
			this.frames = frames;
	}
	
	Menu setFrameColors(int titleCol, int midCol, int sideCol) {
		if(frames == null) return this;
		for(Frame frame: frames)
			frame.setColors(titleCol, midCol, sideCol);
		
		return this;
	}
	
	public Menu getParent() { return parent; }
	public MenuData getMenuType() { return menuData; }
	
	int getSelection() { return selection; }
	ListEntry[] getEntries() { return entries; }
	int getNumEntries() { return entries.length; }
	
	void setSelectedEntry(ListEntry entry) {
		if(mutable && selectionExists())
			entries[selection] = entry;
	}
	
	private boolean selectionExists() {
		return entries != null && selection >= 0 && getNumEntries() > selection;
	}
	
	public void tick(InputHandler input) {
		ticks++;
		if(parent != this) {
			boolean auto = menuData.autoExitDelay() > 0;
			if (!auto && input.getKey(exitKey).clicked || auto && ticks > menuData.autoExitDelay()) {
				Game.setMenu(parent);
				return;
			}
		}
		
		if(entries.length > 0) {
			int prevSel = selection;
			
			if (input.getKey("up").clicked) selection--;
			if (input.getKey("down").clicked) selection++;
			
			selection = selection % entries.length;
			
			while(selection < 0) selection += entries.length;
			
			if (prevSel != selection) {
				Sound.select.play();
				//later: menuData.onSelectionChange(entries[selection]);
			}
		}
		
		menuData.tick(input); // allows handling of own inputs
		
		if(selectionExists())
			entries[selection].tick(input, this);
	}
	
	void updateEntries() {
		entries = menuData.getEntries();
		
		if(entries.length == 0)
			selection = -1;
		else if(selection >= entries.length)
			selection = entries.length - 1;
		else if(selection < 0)
			selection = 0;
	}
	
	protected void renderFrames(Screen screen) {
		for(Frame frame: frames)
			frame.render(screen);
	}
	
	public void render(Screen screen) {
		renderFrames(screen);
		menuData.render(screen); // draws frame, any background stuff; timing can be checked in other ways
		
		renderEntries(screen, selection, entries);
		
		menuData.renderPopup(screen);
	}
	
	void renderEntries(Screen screen, int selection, ListEntry[] entries) {
		//Point anchor = menuData.getAnchor();
		MenuData.Centering centering = menuData.getCentering();
		int spacing = menuData.getSpacing();
		
		// TODO here, deal with centering
		Point anchor = centering.anchor;
		int menu = centering.menu.getVal();
		// menu % 3 == 2 -> menu box left of anchor point
		// menu % 3 == 1 -> menu box horizontally centered on anchor point
		// menu % 3 == 0 -> menu box right of anchor point
		
		// menu / 3 == 2 -> menu box above anchor point
		// menu / 3 == 1 -> menu box vertically centered on anchor point
		// menu / 3 == 0 -> menu box below anchor point
		
		Dimension displayDim = getEntryListDisplaySize(entries, spacing);
		int mx = anchor.x - displayDim.width/2 * (menu % 3);
		int my = anchor.y - displayDim.height/2 * (menu / 3);
		int ly = my;
		//int ycentering = Math.abs(3 - (menu % 3)) % 3; // I have to flip it, or go through array backwards.
		for(int i = 0; i < entries.length; i++) {
			int line = centering.line.ordinal();
			// line % 3 == 0 -> entry left-justified in menu box
			// line % 3 == 1 -> entry centered in menu box
			// line % 3 == 2 -> entry right-justified in menu box
			
			int lx = mx + (displayDim.width - entries[i].getWidth())/2 * (line % 3);
			
			entries[i].render(screen, lx, ly, i == selection);
			
			ly += entries[i].getHeight() + spacing;
		}
	}
	
	public static Dimension getEntryListDisplaySize(ListEntry[] entries, int spacing) {
		int th = 0, tw = 0;
		
		for(int i = 0; i < entries.length; i++) {
			int width = entries[i].getWidth();
			int height = entries[i].getHeight();
			th += height + spacing;
			tw = Math.max(tw, width);
		}
		
		th -= spacing;
		
		return new Dimension(tw, th);
	}
}