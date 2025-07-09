import phasorpy.phasor
import sys
import numpy as np
import phasorpy.io
import tifffile
from phasorpy.plot import plot_phasor_image
from phasorpy.plot import plot_phasor



def main(filename):
    signals = phasorpy.io.signal_from_sdt(filename)
    frequency = signals.attrs['frequency']
    mean, real, imag = phasorpy.phasor.phasor_from_signal(signals, axis='H')
    plot_phasor(
        real, imag,
        frequency=frequency,
        title='Calibrated, filtered phasor coordinates')
    plot_phasor_image(mean, real, imag)
    phasor_data = np.stack([mean, real, imag], axis=0).astype(np.float32)
    tifffile.imwrite("phasor_components.tif", phasor_data)



if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python your_script.py path_to_file.sdt")
    else:
        main(sys.argv[1])
