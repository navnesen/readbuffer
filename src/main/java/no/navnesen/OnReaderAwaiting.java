package no.navnesen;

public interface OnReaderAwaiting<T> {
	Task<Void> run(ReadBuffer<T> buffer);
}
