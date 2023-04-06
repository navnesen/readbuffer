package no.navnesen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReadBuffer<T> {

	protected final Mutex<List<T>> buffer = new Mutex<>(new ArrayList<>());
	protected final Mutex<List<Task<Optional<T>>>> readers = new Mutex<>(new ArrayList<>());

	private final Mutex<OnReaderAwaiting<T>> _onReaderAwaiting = new Mutex<>(null);

	public ReadBuffer<T> setReaderAwaitingListener(OnReaderAwaiting<T> listener) {
		this._onReaderAwaiting.set(listener);
		return this;
	}

	public ReadBuffer<T> setReaderAwaitingListener(OnReaderAwaitingSync<T> listener) {
		this._onReaderAwaiting.set((buffer) -> new Task<>((TaskAction<Void>) () -> {
			listener.run(buffer);
			return null;
		}));
		return this;
	}

	public Task<Optional<T>> read() {
		return new Task<>(() -> {
			Task<Optional<T>> reader;
			try (var buffer = this.buffer.lock()) {
				if (buffer.get().size() > 0) {
					return Optional.ofNullable(buffer.get().remove(0));
				} else {
					try (var readers = this.readers.lock()) {
						reader = new Task<>();
						readers.get().add(reader);
					}
				}
			}
			return this._onReaderAwaiting().and(v -> reader).await();
		});
	}

	public Task<Void> write(T value) {
		return new Task<>(() -> {
			try (var buffer = this.buffer.lock()) {
				boolean shouldBuffer = true;
				try (var readers = this.readers.lock()) {
					if (readers.get().size() > 0) {
						readers.get().remove(0).completed(Optional.ofNullable(value));
						shouldBuffer = false;
					}
				}
				if (shouldBuffer) {
					buffer.get().add(value);
				}
			}
			return null;
		});
	}

	private Task<Void> _onReaderAwaiting() {
		return new Task<>(() -> {
			try (var listener = this._onReaderAwaiting.lock()) {
				if (listener.get() != null) {
					listener.get().run(this).await();
				}
			}
			return null;
		});
	}

}
