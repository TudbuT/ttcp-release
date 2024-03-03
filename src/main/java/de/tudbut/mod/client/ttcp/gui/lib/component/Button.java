package de.tudbut.mod.client.ttcp.gui.lib.component;

public class Button extends Component {
    
    private final ClickEvent event;
    
    {green = true;}
    
    public Button(String s, ClickEvent event) {
        this.text = s;
        this.event = event;
    }
    
    @Override
    public void click(int x, int y, int mouseButton) {
        super.click(x, y, mouseButton);
        if(mouseButton == 0) {
            green = true;
            event.click(this);
        }
    }
    
    public interface ClickEvent {
        void click(Button it);
    }
}
