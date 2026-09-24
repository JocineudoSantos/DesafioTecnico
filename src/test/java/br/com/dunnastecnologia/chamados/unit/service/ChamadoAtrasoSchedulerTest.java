package br.com.dunnastecnologia.chamados.unit.service;

import br.com.dunnastecnologia.chamados.infrastructure.repository.ChamadoRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.ChamadoAtrasoScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChamadoAtrasoSchedulerTest {

    @Mock
    private ChamadoRepository chamadoRepository;

    @InjectMocks
    private ChamadoAtrasoScheduler chamadoAtrasoScheduler;

    @Test
    void marcarChamadosAtrasadosDeveExecutarAtualizacaoEmLote() {
        chamadoAtrasoScheduler.marcarChamadosAtrasados();

        verify(chamadoRepository).marcarChamadosAtrasados();
    }
}
