package no.navnesen;

public interface OnReaderAwaitingSync<T> {
	void run(ReadBuffer<T> buffer) throws Exception;
}
