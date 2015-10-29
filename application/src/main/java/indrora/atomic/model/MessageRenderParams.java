package indrora.atomic.model;

public final class MessageRenderParams {
  public String colorScheme = "";
  public boolean timestamps = false;
  public boolean icons = false;
  public boolean mircColors = false;
  public boolean nickColors = false;
  public boolean useDarkScheme = false;
  public boolean smileys = false;
    public boolean timestamp24Hour;
    public boolean timestampSeconds;
    public boolean messageColors;

    @Override
  public boolean equals(Object o) {
    if( o instanceof MessageRenderParams ) {
      MessageRenderParams t = (MessageRenderParams)o;
      return this.colorScheme.equals(t.colorScheme) &&
          this.timestamps ==      t.timestamps &&
          this.icons ==           t.icons &&
          this.mircColors ==   t.mircColors &&
          this.nickColors ==      t.nickColors &&
          this.useDarkScheme ==   t.useDarkScheme &&
          this.smileys ==         t.smileys &&
          this.timestamp24Hour == t.timestamp24Hour &&
          this.timestampSeconds == t.timestampSeconds &&
          this.messageColors == t.messageColors;
    } else {
      return false;
    }
  }

}
