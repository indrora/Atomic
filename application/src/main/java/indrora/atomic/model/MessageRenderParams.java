package indrora.atomic.model;

public final class MessageRenderParams {
  public String colorScheme = "";
  public boolean timestamps = false;
  public boolean icons = false;
  public boolean messageColors = false;
  public boolean nickColors = false;
  public boolean useDarkScheme = false;
  public boolean smileys = false;

  @Override
  public boolean equals(Object o) {
    if( o instanceof MessageRenderParams ) {
      MessageRenderParams t = (MessageRenderParams)o;
      return this.colorScheme ==  t.colorScheme &&
          this.timestamps ==      t.timestamps &&
          this.icons ==           t.icons &&
          this.messageColors ==   t.messageColors &&
          this.nickColors ==      t.nickColors &&
          this.useDarkScheme ==   t.useDarkScheme &&
          this.smileys ==         t.smileys;
    } else {
      return false;
    }
  }

}
