public class Card {
    private CardColor color;
    private CardValue value;

    public Card(CardColor color, CardValue value) {
        this.color = color;
        this.value = value;
    }

    public CardColor getColor() {
        return color;
    }
    public CardValue getValue() {
        return value;
    }
    public void setValue(CardValue value) {
        this.value = value;
    }
    public void setColor(CardColor color) {
        this.color = color;
    }

    public String print() {
        if (value == CardValue.EMPTY)
        {
            return String.format("%s", color);
        } else {
            return String.format("%s %s", color, value);
        }
    }
}


