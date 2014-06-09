package indrora.atomic.utils;

/**
 * LatchingValue: A simple trippable value.
 * 
 * Latching Values have 2 states: Default and Latched. Whenever the "current" value is read,
 * the state becomes "latched", thus changing the value.
 * 
 * Latched values however can be "un-tripped" (reset) to their default value, in case something does need
 * to reset this "trap"
 * 
 * @author indrora
 *
 * @param <E>
 */
public class LatchingValue<E> {
	E currentValue;
	final E defaultValue;
	final E latchedValue;
	/**
	 * Default constructor.
	 * @param defaultValue Default value of the latch
 	 * @param latchedValue Tripped value of the latch
	 */
	public LatchingValue(E defaultValue, E latchedValue)
	{
		this.defaultValue = defaultValue;
		this.currentValue = this.defaultValue;
		this.latchedValue = latchedValue;
	}
	/**
	 * Resets the latch state.
	 */
	public void reset()
	{
		this.currentValue = this.defaultValue;
	}
	/**
	 * Gets the current value of the latch.
	 * 
	 * This will trip the latch.
	 * 
	 * @return The value of the latch.
	 */
	public E getValue()
	{
		E tmp = this.currentValue;
		this.currentValue = this.latchedValue;
		return tmp;
	}
}
