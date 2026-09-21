"""Generate the Thor Companion launcher icon (a Poké Ball on an emerald gradient).

Produces legacy PNG mipmaps for every density without any third-party
dependency (pure stdlib PNG encoder).
"""
import struct
import zlib
from pathlib import Path

RES = Path(r"C:\Repos\ThorCompanion\app\src\main\res")

GRAD_START = (27, 94, 32)    # #1B5E20
GRAD_END = (10, 46, 18)      # #0A2E12
RED = (229, 57, 53)          # #E53935
WHITE = (255, 255, 255)
BLACK = (20, 20, 20)
GRAY = (198, 198, 198)


def _mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def render(size: int, ss: int = 3):
    """Render a size x size icon with ss-supersampled antialiasing."""
    big = size * ss
    cx = cy = big / 2.0
    ball_r = big * 0.40
    band_half = big * 0.045
    button_r = big * 0.155
    ring_w = big * 0.045
    dot_r = big * 0.065

    buf = [[(0, 0, 0, 0)] * big for _ in range(big)]
    for y in range(big):
        py = y + 0.5
        for x in range(big):
            px = x + 0.5
            # gradient background
            t = (px + py) / (2 * big)
            col = _mix(GRAD_START, GRAD_END, t)
            dx = px - cx
            dy = py - cy
            dist = (dx * dx + dy * dy) ** 0.5
            if dist <= ball_r:
                if dist <= button_r:
                    col = GRAY if dist <= dot_r else WHITE
                elif dist <= button_r + ring_w:
                    col = BLACK
                elif abs(dy) <= band_half:
                    col = BLACK
                elif dy < 0:
                    col = RED
                else:
                    col = WHITE
            buf[y][x] = (col[0], col[1], col[2], 255)

    # box downsample
    out = []
    for y in range(size):
        row = []
        for x in range(size):
            r = g = b = a = 0
            for sy in range(ss):
                for sx in range(ss):
                    c = buf[y * ss + sy][x * ss + sx]
                    r += c[0]; g += c[1]; b += c[2]; a += c[3]
            n = ss * ss
            row.append((r // n, g // n, b // n, a // n))
        out.append(row)
    return out


def write_png(path: Path, size: int, pixels):
    raw = bytearray()
    for row in pixels:
        raw.append(0)  # filter none
        for (r, g, b, a) in row:
            raw += bytes((r, g, b, a))

    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        c += struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        return c

    ihdr = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", ihdr)
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def main():
    targets = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192,
    }
    for density, size in targets.items():
        out = RES / ("mipmap-" + density) / "ic_launcher.png"
        write_png(out, size, render(size))
        print(f"{density}: {out} ({size}x{size})")


if __name__ == "__main__":
    main()
