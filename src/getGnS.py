import phasorpy.phasor
import sys
import numpy as np
import phasorpy.io
from phasorpy.plot import plot_phasor_image
from phasorpy.plot import plot_phasor



def main():
    signal = phasorpy.io.signal_from_imspector_tiff(r'C:\Users\hdoan3\Downloads\Embryo.tif')
    frequency = signal.attrs['frequency']
    mean, real, imag = phasorpy.phasor.phasor_from_signal(signal, axis='H')

    reference_signal = phasorpy.io.signal_from_imspector_tiff(r'C:\Users\hdoan3\Downloads\Fluorescein_Embryo.tif')
    assert reference_signal.attrs['frequency'] == frequency
    reference_mean, reference_real, reference_imag = phasorpy.phasor.phasor_from_signal( reference_signal, axis=0)
    real, imag = phasorpy.phasor.phasor_calibrate(
    real,
    imag,
    reference_mean,
    reference_real,
    reference_imag,
    frequency=frequency,
    lifetime=4.2,
    )



    plot_phasor(
        real, imag,
        frequency=frequency,
        title='Calibrated, filtered phasor coordinates')
    plot_phasor_image(mean, real, imag)
    

if __name__ == "__main__":
    main();
