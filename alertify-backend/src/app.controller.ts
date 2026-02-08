import { Controller, Get } from '@nestjs/common';
import { ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { AppService } from './app.service';

@ApiTags('General')
@Controller()
export class AppController {
  constructor(private readonly appService: AppService) {}

  @ApiOperation({ summary: 'Verificar que el API está en línea' })
  @ApiResponse({ status: 200, description: 'API funcionando correctamente' })
  @Get()
  getHello(): string {
    return this.appService.getHello();
  }

  @ApiOperation({ summary: 'Obtener estado general del sistema' })
  @ApiResponse({ status: 200, description: 'Estado del sistema obtenido correctamente' })
  @Get('status')
  getStatus(): any {
    return this.appService.getStatus();
  }
}
