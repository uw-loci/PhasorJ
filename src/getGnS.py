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
    
    mean_float_array = "float[][] mean = {\n"
    for row in mean:
        row_str = ", ".join(f"Float.NaN" if np.isnan(x) else f"{x:.8f}f" for x in row )
        mean_float_array +=  f"    {{{row_str}}},\n"
    mean_float_array += "};" 

    with open("mean_float_array.txt", "w") as file:
        file.write(mean_float_array)

    g_float_array = "float[][] g = {\n"
    for row in real:
        row_str = ", ".join(f"Float.NaN" if np.isnan(x) else f"{x:.8f}f" for x in row )
        g_float_array +=  f"    {{{row_str}}},\n"
    g_float_array += "};" 

    with open("g_float_array.txt", "w") as file:
        file.write(g_float_array)

    s_float_array = "float[][] s = {\n"
    for row in imag:
        row_str = ", ".join(f"Float.NaN" if np.isnan(x) else f"{x:.8f}f" for x in row )
        s_float_array +=  f"    {{{row_str}}},\n"
    s_float_array += "};" 

    with open("s_float_array.txt", "w") as file:
        file.write(s_float_array)

if __name__ == "__main__":
    main();
